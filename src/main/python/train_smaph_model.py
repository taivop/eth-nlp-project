#!/usr/bin/env python
"""Utility to train and pickle a SMAPH classifier model."""

from __future__ import print_function

import pickle
import sys

import numpy as np
from sklearn.linear_model import SGDClassifier
from sklearn.svm import SVC

from sklearn.decomposition import PCA

import matplotlib.pyplot as plt

from multiprocessing import Process, Queue

from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import AdaBoostClassifier
from sklearn.neighbors import KNeighborsClassifier

from sklearn.cross_validation import  cross_val_predict, KFold

from sklearn.metrics import confusion_matrix

from data_util import impute_nan_inf, load_training_data, rescale, load_dataset, pca_components, load_dataset_pca

# pylint: disable=missing-docstring, invalid-name

# TODO(andrei): This should be in a common config.
FEATURE_COUNT = 20


def usage():
    print("Usage: train_smaph_model.py <csv_file> <dest_pickle_file> <C>")
    exit(1)


def pickle_check(pickle_file, X_raw, y_raw):
    # TODO(andrei): Unified preprocessing function (e.g. use sklearn pipeline directly).
    loaded_clf, ranges, means, scaler = pickle.load(pickle_file)
    X_raw = impute_nan_inf(X_raw)
    X = scaler.transform((X_raw - means) / ranges)
    y = y_raw
    y_pred = loaded_clf.predict(X)
    print(confusion_matrix(y, y_pred))

def train_svm(x_train,
              y_train,
              x_valid,
              y_valid,
              loss,
              penalty,
              alpha,
              n_iter):

    full_svm_classifier = SGDClassifier(loss=loss,
                                        penalty=penalty,
                                        alpha=alpha,
                                        verbose=True,
                                        class_weight='balanced',
                                        n_iter=n_iter,
                                        learning_rate="optimal")
    full_svm_classifier.fit(x_train, y_train)

    cm = confusion_matrix(y_valid,full_svm_classifier.predict(x_valid))

    accuracy_negative = cm[0,0] / np.sum(cm[0,:])
    accuracy_positive = cm[1,1] / np.sum(cm[1,:])

    precision = cm[1,1] / (cm[1,1] + cm[0,1])
    recall = cm[1,1] / (cm[1,1] + cm[1,0])

    f1_score = 2 * precision * recall / (precision + recall)

    print(accuracy_positive,accuracy_negative,precision,recall,f1_score)

    return full_svm_classifier

def eval_svm(svm_classifier,x,y):
    y_bar = svm_classifier.predict(x)

    accuracy = 1.0 - (np.sum(y_bar != y) / np.shape(y))

    return accuracy

def train_ada_boost(x_train,
                    y_train,
                    n_estimators,
                    learning_rate,
                    max_tree_depth):

    tree_classifier = DecisionTreeClassifier(max_depth=max_tree_depth,
                                             class_weight="balanced")


    ada_boost_classifier = AdaBoostClassifier(base_estimator=tree_classifier,
                                              n_estimators=n_estimators,
                                              learning_rate=learning_rate)

    ada_boost_classifier.fit(x_train,y_train)

    return ada_boost_classifier

def eval_ada_boost(ada_boost_classifier,
                   x_train,
                   y_train,
                   x_valid,
                   y_valid):

    x_train_pos = x_train[y_train == 1,:]
    y_train_pos = y_train[y_train == 1]

    x_train_neg = x_train[y_train == 0,:]
    y_train_neg = y_train[y_train == 0]

    eval_train_pos = list(ada_boost_classifier.staged_score(x_train_pos,y_train_pos))
    eval_train_neg = list(ada_boost_classifier.staged_score(x_train_neg,y_train_neg))

    x_valid_pos = x_valid[y_valid == 1,:]
    y_valid_pos = y_valid[y_valid == 1]

    x_valid_neg = x_valid[y_valid == 0,:]
    y_valid_neg = y_valid[y_valid == 0]

    eval_valid_pos = list(ada_boost_classifier.staged_score(x_valid_pos,y_valid_pos))
    eval_valid_neg = list(ada_boost_classifier.staged_score(x_valid_neg,y_valid_neg))

    return eval_train_pos, eval_train_neg, eval_valid_pos, eval_valid_neg

def ada_boost_cv(x_train,
                 y_train,
                 cv,
                 max_tree_depth,
                 n_estimators,
                 learning_rate):

    tree_classifier = DecisionTreeClassifier(max_depth=max_tree_depth,
                                             class_weight="balanced")


    ada_boost_classifier = AdaBoostClassifier(base_estimator=tree_classifier,
                                              n_estimators=n_estimators,
                                              learning_rate=learning_rate)

    y_bar = cross_val_predict(estimator=ada_boost_classifier,
                              X=x_train,
                              y=y_train,
                              cv=cv,
                              n_jobs=cv)

    y_bar_proba = ada_boost_classifier.predict_proba(x_train)
    print(list(zip(y_bar,y_bar_proba)))

    cm = confusion_matrix(y_train,y_bar)

    accuracy_negative = cm[0,0] / np.sum(cm[0,:])
    accuracy_positive = cm[1,1] / np.sum(cm[1,:])

    precision = cm[1,1] / (cm[1,1] + cm[0,1])
    recall = cm[1,1] / (cm[1,1] + cm[1,0])

    f1_score = 2 * precision * recall / (precision + recall)

    return accuracy_positive, accuracy_negative, precision, recall, f1_score

def classifier_cv_process(classifier,
                          x_train,
                          y_train,
                          x_test,
                          y_test,
                          pruning_threshold,
                          queue):

        classifier.fit(x_train, y_train)

        y_bar_proba = classifier.predict_proba(X=x_test)
        y_bar = np.array(y_bar_proba[:,1]>pruning_threshold,dtype=int)

        cm = confusion_matrix(y_test,y_bar)

        queue.put(cm)

def ada_boost_cv_proba(x_train,
                       y_train,
                       cv,
                       max_tree_depth,
                       n_estimators,
                       learning_rate,
                       pruning_threshold):

    tree_classifier = DecisionTreeClassifier(max_depth=max_tree_depth,
                                             class_weight="balanced")


    ada_boost_classifier = AdaBoostClassifier(base_estimator=tree_classifier,
                                              n_estimators=n_estimators,
                                              learning_rate=learning_rate)

    N = x_train.shape[0]

    kfold = KFold(n=N,n_folds=cv,shuffle=True)

    cm_total = np.zeros([2,2])

    queue = Queue()

    ada_boost_cv_processes = []

    for train_ids, test_ids in kfold:
        x_train_cv = x_train[train_ids,:]
        y_train_cv = y_train[train_ids]

        x_test_cv = x_train[test_ids,:]
        y_test_cv = y_train[test_ids]

        ada_boost_cv_processes += [Process(target=classifier_cv_process, args=(ada_boost_classifier,
                                                                               x_train_cv,
                                                                               y_train_cv,
                                                                               x_test_cv,
                                                                               y_test_cv,
                                                                               pruning_threshold,
                                                                               queue))]

    for process in ada_boost_cv_processes:
        process.start()

    for process in ada_boost_cv_processes:
        cm_total += queue.get()
        process.join()

    accuracy_negative = cm_total[0,0] / np.sum(cm_total[0,:])
    accuracy_positive = cm_total[1,1] / np.sum(cm_total[1,:])

    precision = cm_total[1,1] / (cm_total[1,1] + cm_total[0,1])
    recall = cm_total[1,1] / (cm_total[1,1] + cm_total[1,0])

    f1_score = 2 * precision * recall / (precision + recall)

    return accuracy_positive, accuracy_negative, precision, recall, f1_score

def plot_ada_boost(eval_train_pos,eval_train_neg, eval_valid_pos, eval_valid_neg, image_file):
    n_estimators = len(eval_train_pos)

    plt.figure()
    plt.plot(range(n_estimators+1)[1:],eval_train_pos,c="y",linewidth=2,label="Training Set, Positive Samples Accuracy")
    plt.plot(range(n_estimators+1)[1:],eval_train_neg,c="m",linewidth=2,label="Training Set, Negative Samples Accuracy")
    plt.plot(range(n_estimators+1)[1:],eval_valid_pos,c="b",linewidth=2,label="Validation Set, Positive Samples Accuracy")
    plt.plot(range(n_estimators+1)[1:],eval_valid_neg,c="r",linewidth=2,label="Validation Set, Negative Samples Accuracy")
    plt.ylim([-0.1,1.1])
    plt.grid()
    #plt.legend(bbox_to_anchor=(0, 1), loc='upper center', ncol=1)
    plt.savefig(image_file)

def plot_classification_boundaries(X,
                                   y,
                                   plot_step,
                                   classifier,
                                   class_names,
                                   plot_colors):
    plt.figure()
    x_min, x_max = X[:, 0].min() - 0.01, X[:, 0].max() + 0.01
    y_min, y_max = X[:, 1].min() - 0.01, X[:, 1].max() + 0.01
    xx, yy = np.meshgrid(np.arange(x_min, x_max, plot_step),
                         np.arange(y_min, y_max, plot_step))

    Z = classifier.predict(np.c_[xx.ravel(), yy.ravel()])
    Z = Z.reshape(xx.shape)
    plt.contourf(xx, yy, Z, cmap=plt.cm.Paired)
    plt.axis("tight")

    # Plot the training points
    for i, n, c in zip(range(2), class_names, plot_colors):
        idx = np.where(y == i)
        plt.scatter(X[idx, 0], X[idx, 1],
                    c=c, cmap=plt.cm.Paired,
                    label="Class %s" % n,s=8)
    plt.xlim(x_min, x_max)
    plt.ylim(y_min, y_max)
    plt.legend(loc='upper right')
    plt.xlabel('x')
    plt.ylabel('y')
    plt.title('Decision Boundary')

    plt.savefig("ada_boost_classification_boundaries.png")

    plt.figure()
    plt.contourf(xx, yy, Z, cmap=plt.cm.Paired)
    plt.axis("tight")

    idx = np.where(y == 0)
    plt.scatter(X[idx, 0], X[idx, 1],
                c='b', cmap=plt.cm.Paired,
                label="Class %s" % '0',s=8)

    plt.xlim(x_min, x_max)
    plt.ylim(y_min, y_max)
    plt.legend(loc='upper right')
    plt.xlabel('x')
    plt.ylabel('y')
    plt.title('Decision Boundary (Negative)')

    plt.savefig("ada_boost_classification_boundaries_negative.png")

    plt.figure()
    plt.contourf(xx, yy, Z, cmap=plt.cm.Paired)
    plt.axis("tight")

    idx = np.where(y == 1)
    plt.scatter(X[idx, 0], X[idx, 1],
                c='r', cmap=plt.cm.Paired,
                label="Class %s" % '1',s=8)

    plt.xlim(x_min, x_max)
    plt.ylim(y_min, y_max)
    plt.legend(loc='upper right')
    plt.xlabel('x')
    plt.ylabel('y')
    plt.title('Decision Boundary (Positive)')

    plt.savefig("ada_boost_classification_boundaries_positive.png")

def classification_tree_cv_proba(x_train,
                                 y_train,
                                 cv,
                                 max_tree_depth,
                                 pruning_threshold):

    tree_classifier = DecisionTreeClassifier(max_depth=max_tree_depth,
                                             class_weight="balanced")

    N = x_train.shape[0]

    kfold = KFold(n=N,n_folds=cv,shuffle=True)

    cm_total = np.zeros([2,2])

    queue = Queue()

    decision_tree_cv_processes = []

    for train_ids, test_ids in kfold:
        x_train_cv = x_train[train_ids,:]
        y_train_cv = y_train[train_ids]

        x_test_cv = x_train[test_ids,:]
        y_test_cv = y_train[test_ids]

        decision_tree_cv_processes += [Process(target=classifier_cv_process, args=(tree_classifier,
                                                                                   x_train_cv,
                                                                                   y_train_cv,
                                                                                   x_test_cv,
                                                                                   y_test_cv,
                                                                                   pruning_threshold,
                                                                                   queue))]

    for process in decision_tree_cv_processes:
        process.start()

    for process in decision_tree_cv_processes:
        cm_total += queue.get()
        process.join()

    accuracy_negative = cm_total[0,0] / np.sum(cm_total[0,:])
    accuracy_positive = cm_total[1,1] / np.sum(cm_total[1,:])

    precision = cm_total[1,1] / (cm_total[1,1] + cm_total[0,1])
    recall = cm_total[1,1] / (cm_total[1,1] + cm_total[1,0])

    f1_score = 2 * precision * recall / (precision + recall)

    return accuracy_positive, accuracy_negative, precision, recall, f1_score

def train_knn(x_train,
              y_train,
              n_neighbors):
    knn = KNeighborsClassifier(n_neighbors=n_neighbors)
    knn.fit(x_train,y_train)

    return knn

def eval_knn(knn,
             x_train,
             y_train,
             x_valid,
             y_valid):

    x_train_pos = x_train[y_train == 1,:]
    y_train_pos = y_train[y_train == 1]

    eval_train_pos = knn.score(x_train_pos,y_train_pos)

    x_train_neg = x_train[y_train == 0,:]
    y_train_neg = y_train[y_train == 0]

    eval_train_neg = knn.score(x_train_neg,y_train_neg)

    x_valid_pos = x_valid[y_valid == 1,:]
    y_valid_pos = y_valid[y_valid == 1]

    eval_valid_pos = knn.score(x_valid_pos,y_valid_pos)

    x_valid_neg = x_valid[y_valid == 0,:]
    y_valid_neg = y_valid[y_valid == 0]

    eval_valid_neg = knn.score(x_valid_neg,y_valid_neg)

    return eval_train_pos, eval_train_neg, eval_valid_pos, eval_valid_neg

def train_knn_iter(x_train,
                   y_train,
                   x_valid,
                   y_valid,
                   n_neighbors_list):

    eval_train_pos_list = []
    eval_train_neg_list = []
    eval_valid_pos_list = []
    eval_valid_neg_list = []

    for n_neighbors in n_neighbors_list:
        knn = train_knn(x_train=x_train,
                        y_train=y_train,
                        n_neighbors=n_neighbors)

        eval_train_pos, eval_train_neg, eval_valid_pos, eval_valid_neg = \
            eval_knn(knn=knn,
                     x_train=x_train,
                     y_train=y_train,
                     x_valid=x_valid,
                     y_valid=y_valid)

        eval_train_pos_list += [eval_train_pos]
        eval_train_neg_list += [eval_train_neg]
        eval_valid_pos_list += [eval_valid_pos]
        eval_valid_neg_list += [eval_valid_neg]

    return eval_train_pos_list, eval_train_neg_list, eval_valid_pos_list, eval_valid_neg_list

def plot_knn(eval_train_pos_list,
             eval_train_neg_list,
             eval_valid_pos_list,
             eval_valid_neg_list,
             n_neighbors_list,
             image_file):

    plt.figure()
    plt.plot(n_neighbors_list,eval_train_pos_list,c="y",linewidth=2,label="Training Set, Positive Samples Accuracy")
    plt.plot(n_neighbors_list,eval_train_neg_list,c="m",linewidth=2,label="Training Set, Negative Samples Accuracy")
    plt.plot(n_neighbors_list,eval_valid_pos_list,c="b",linewidth=2,label="Validation Set, Positive Samples Accuracy")
    plt.plot(n_neighbors_list,eval_valid_neg_list,c="r",linewidth=2,label="Validation Set, Negative Samples Accuracy")
    plt.ylim([-0.1,1.1])
    plt.grid()
    #plt.legend(bbox_to_anchor=(0, 1), loc='upper center', ncol=1)
    plt.savefig(image_file)

def pca_visualize(x_train,y_train,n_components):
    x_train_t = x_train.T

    pca = PCA(n_components=n_components)
    pca.fit(x_train_t)

    for i in range(n_components):
        explained_variance = np.sum(pca.explained_variance_ratio_[:(i+1)])
        # print("Principal components = " + str(i+1) + " Explained variance = " + str(explained_variance))

    x_train_pca = pca.components_.T

    x_pca_pos = x_train_pca[y_train == 1,:]
    x_pca_neg = x_train_pca[y_train == 0,:]

    plt.figure()
    plt.scatter(x=x_pca_neg[:,0],y=x_pca_neg[:,1],c="y",marker="o",s=4)
    plt.scatter(x=x_pca_pos[:,0],y=x_pca_pos[:,1],c="b",marker="o",s=4)
    # plt.xlim([-0.025,0.015])
    # plt.ylim([-0.02,0.04])
    plt.grid()
    plt.savefig("pca_1_2.png")

    plt.figure()
    plt.scatter(x=x_pca_neg[:,0],y=x_pca_neg[:,2],c="y",marker="o",s=4)
    plt.scatter(x=x_pca_pos[:,0],y=x_pca_pos[:,2],c="b",marker="o",s=4)
    #plt.xlim([-0.1,0.1])
    #plt.ylim([-0.5,0.5])
    plt.grid()
    plt.savefig("pca_1_3.png")

    plt.figure()
    plt.scatter(x=x_pca_neg[:,1],y=x_pca_neg[:,2],c="y",marker="o",s=4)
    plt.scatter(x=x_pca_pos[:,1],y=x_pca_pos[:,2],c="b",marker="o",s=4)
    #plt.xlim([-0.1,0.1])
    #plt.ylim([-0.5,0.5])
    plt.grid()
    plt.savefig("pca_2_3.png")

# serializes classifier object to binary file
def store_classifier(object, filename):
    with open(filename,"wb") as fid:
        pickle.dump(object, fid)

# loads classifier object from binary file
def load_classifier(filename):
    with open(filename,"rb") as fid:
        object = pickle.load(fid)

    return object

def main():
    if len(sys.argv) != 4:
        usage()

    print("Will train SVM. Assuming every training data point has {0} "
          "features.".format(FEATURE_COUNT))
    csv_file = sys.argv[1]
    dest_pickle_file = sys.argv[2]
    C = float(sys.argv[3])

    # A simple linear (for the time being) SVM classifier using the optimal
    # parameters established via grid search in the notebook.
    # clf = SGDClassifier(class_weight='balanced', loss='hinge', penalty='l1',
                        # alpha=0.05)

    # The non-linear version. Much more expensive to train, but yields somewhat
    # better results, and corresponds to what is described in the paper.
    clf = SVC(C=C, class_weight='balanced')
    print("Will read data from {0} and write the pickled model to "
          "{1}.".format(csv_file, dest_pickle_file))
    X_raw, y_raw = load_training_data(csv_file, FEATURE_COUNT)
    X, y, ranges, means, scaler = rescale(X_raw, y_raw)

    print("Read and processed {0} data points.".format(X.shape[0]))
    print("Will fit classifier:\n{0}".format(clf))

    clf.fit(X, y)

    print("Dumping pickle together with scaling info.")
    print("Structure will be (classifier, feature_magnitudes, feature_means, sklearn_scaler).")
    pickle.dump((clf, ranges, means, scaler), open(dest_pickle_file, 'wb'))
    print("Dump complete.")

    print("Validating pickle.")
    pickle_check(open(dest_pickle_file, 'rb'), X_raw, y_raw)

def main2():
    X_raw, y_raw = load_training_data(csv_file_name="../../../data/all-candidates-5-24-20-26.csv",
                                      feature_count=24)


    X, y, ranges, means, scaler = rescale(X=X_raw,y=y_raw)

    ada_boost_classifier = train_ada_boost(x_train=X,
                                           y_train=y,
                                           n_estimators=100,
                                           max_tree_depth=3,
                                           learning_rate=0.1)

    store_classifier((ada_boost_classifier,ranges,means,scaler),"ada_boost_est_100_tree_depth_3_new.pkl")
    '''

    # n_components_list = range(10,24)
    # max_tree_depth_list = [1,2,3,4,5]
    # threshold_list = list(np.linspace(0.51,0.52,2))

    pruning_threshold_list = np.linspace(0.5,0.6,6)
    n_estimators_list = [50,100]
    max_tree_depth_list = [1,2,3,4,5]

    for max_tree_depth in max_tree_depth_list:
        for n_estimators in n_estimators_list:
            for pruning_threshold in pruning_threshold_list:

                X_pca = pca_components(x=X,n_components=20)

                accuracy_positive, accuracy_negative, precision, recall, f1_score = \
                    ada_boost_cv_proba(x_train=X_pca,
                                       y_train=y,
                                       cv=4,
                                       max_tree_depth=max_tree_depth,
                                       n_estimators=n_estimators,
                                       learning_rate=0.1,
                                       pruning_threshold=pruning_threshold)
                print(max_tree_depth,n_estimators,pruning_threshold,accuracy_positive,accuracy_negative,precision,recall,f1_score)
    '''

if __name__ == '__main__':
    main2()
