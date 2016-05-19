#!/usr/bin/env python
"""Utility to train and pickle a SMAPH classifier model."""

from __future__ import print_function

import pickle
import sys

import numpy as np
import sklearn
from sklearn.linear_model import SGDClassifier
from sklearn.metrics import confusion_matrix
from sklearn.svm import SVC

from sklearn.decomposition import PCA

import matplotlib.pyplot as plt

from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import AdaBoostClassifier

from data_util import impute_nan_inf, load_training_data, rescale, load_dataset

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

    valid_error_min = 1.0

    # importance sampling, note the opposite sign!
    pos_weight = np.sum(y_train == 0) / y_train.shape[0]
    neg_weight = np.sum(y_train == 1) / y_train.shape[0]

    svm_classifier = SGDClassifier(loss=loss,
                                   penalty=penalty,
                                   alpha=alpha,
                                   verbose=True,
                                   warm_start=True,
                                   n_iter=n_iter,
                                   class_weight={0:neg_weight,1:pos_weight})

    for iter in range(n_iter):
        svm_classifier.partial_fit(X=x_train, y=y_train, classes=np.unique(y_train))

        train_acc = eval_svm(svm_classifier, x_train, y_train)
        valid_acc = eval_svm(svm_classifier, x_valid, y_valid)

        valid_acc_pos = eval_svm(svm_classifier, x_valid[y_valid == 1,:], y_valid[y_valid == 1])
        valid_acc_neg = eval_svm(svm_classifier, x_valid[y_valid == 0,:], y_valid[y_valid == 0])

        print("iter = " + str(iter+1) + " train acc = " + str(train_acc[0]) + " valid acc = " + str(valid_acc[0]))

        print("iter = " + str(iter+1) + " valid pos acc = " + str(valid_acc_pos[0]) + " valid neg acc = " + str(valid_acc_neg[0]))

        # if lower error achieved in validation set, classifier is dumped to a file
        if (valid_acc > valid_error_min):
            valid_error_min = valid_acc
            #store_svm(svm_classifier,"../../tmp/svm_classifier.pkl")

    #svm_classifier_ret = load_svm("../../tmp/svm_classifier.pkl")

    return svm_classifier

def eval_svm(svm_classifier,x,y):
    y_bar = svm_classifier.predict(x)

    accuracy = 1.0 - (np.sum(y_bar != y) / np.shape(y))

    return accuracy

def train_ada_boost(x_train,
                    y_train,
                    n_estimators,
                    learning_rate):

    tree_classifier = DecisionTreeClassifier(max_depth=10,
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

    #eval_train = list(ada_boost_classifier.staged_score(x_train,y_train))
    #eval_valid = list(ada_boost_classifier.staged_score(x_valid,y_valid))

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

def plot_ada_boost(eval_train_pos,eval_train_neg, eval_valid_pos, eval_valid_neg):
    n_estimators = len(eval_train_pos)

    plt.figure()
    plt.plot(range(n_estimators+1)[1:],eval_train_pos,c="y",linewidth=2,label="Training Set, Positive Samples Accuracy")
    plt.plot(range(n_estimators+1)[1:],eval_train_neg,c="m",linewidth=2,label="Training Set, Negative Samples Accuracy")
    plt.plot(range(n_estimators+1)[1:],eval_valid_pos,c="b",linewidth=2,label="Validation Set, Positive Samples Accuracy")
    plt.plot(range(n_estimators+1)[1:],eval_valid_neg,c="r",linewidth=2,label="Validation Set, Negative Samples Accuracy")
    plt.ylim([-0.1,1.1])
    plt.grid()
    #plt.legend(bbox_to_anchor=(0, 1), loc='upper center', ncol=1)
    plt.show()

def pca_visualize(x_train,y_train):
    x_train_t = x_train.T

    pca = PCA(n_components=20)
    pca.fit(x_train_t)

    for i in range(20):
        explained_variance = np.sum(pca.explained_variance_ratio_[:i])
        print("Principal components = " + str(i+1) + " Explained variance = " + str(explained_variance))

    x_train_pos_t = x_train[y_train == 1,:].T
    x_train_neg_t = x_train[y_train == 0,:].T

    print("x train pos = " + str(x_train_pos_t.shape))
    print("x train neg = " + str(x_train_neg_t.shape))

    pca_pos = PCA(n_components=3)
    pca_pos.fit(x_train_pos_t)

    x_pca_pos = pca_pos.components_

    pca_neg = PCA(n_components=3)
    pca_neg.fit(x_train_neg_t)

    x_pca_neg = pca_neg.components_

    plt.figure()
    plt.scatter(x=x_pca_neg[0,:],y=x_pca_neg[1,:],c="y",marker="o",s=30)
    plt.scatter(x=x_pca_pos[0,:],y=x_pca_pos[1,:],c="b",marker="o",s=30)
    #plt.xlim([-0.1,0.1])
    #plt.ylim([-0.5,0.5])
    plt.grid()
    plt.savefig("pca_1_2.png")

    plt.figure()
    plt.scatter(x=x_pca_neg[0,:],y=x_pca_neg[2,:],c="y",marker="o",s=30)
    plt.scatter(x=x_pca_pos[0,:],y=x_pca_pos[2,:],c="b",marker="o",s=30)
    #plt.xlim([-0.1,0.1])
    #plt.ylim([-0.5,0.5])
    plt.grid()
    plt.savefig("pca_1_3.png")

    plt.figure()
    plt.scatter(x=x_pca_neg[1,:],y=x_pca_neg[2,:],c="y",marker="o",s=30)
    plt.scatter(x=x_pca_pos[1,:],y=x_pca_pos[2,:],c="b",marker="o",s=30)
    #plt.xlim([-0.1,0.1])
    #plt.ylim([-0.5,0.5])
    plt.grid()
    plt.savefig("pca_2_3.png")

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
    x_train,y_train,x_valid,y_valid = load_dataset(csv_file_name="../../../data/all-candidates-5-18-15-13.csv",feature_count=20)
    '''
    train_svm(x_train=x_train,
              y_train=y_train,
              x_valid=x_valid,
              y_valid=y_valid,
              loss="hinge",
              penalty="l2",
              alpha=0.0001,
              n_iter=100)
    '''
    ada_boost_classifier = train_ada_boost(x_train=x_train,
                                           y_train=y_train,
                                           n_estimators=50,
                                           learning_rate=0.1)

    eval_train_pos, eval_train_neg, eval_valid_pos, eval_valid_neg = \
                             eval_ada_boost(ada_boost_classifier=ada_boost_classifier,
                                            x_train=x_train,
                                            y_train=y_train,
                                            x_valid=x_valid,
                                            y_valid=y_valid)

    plot_ada_boost(eval_train_pos=eval_train_pos,
                   eval_train_neg=eval_train_neg,
                   eval_valid_pos=eval_valid_pos,
                   eval_valid_neg=eval_valid_neg)

if __name__ == '__main__':
    main2()
