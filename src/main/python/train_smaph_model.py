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

from data_util import impute_nan_inf, load_training_data, rescale

# pylint: disable=missing-docstring, invalid-name

# TODO(andrei): This should be in a common config.
FEATURE_COUNT = 24

np.random.seed(0xF00BA2)

def usage():
    print("Usage: train_smaph_model.py <csv_file> <dest_pickle_file_prefix> <C> <probabilistic>")
    exit(1)


def pickle_check(pickle_file, X_raw, y_raw):
    # TODO(andrei): Unified preprocessing function (e.g. use sklearn pipeline directly).
    loaded_clf, ranges, means, scaler = pickle.load(pickle_file)
    X_raw = impute_nan_inf(X_raw)
    X = scaler.transform((X_raw - means) / ranges)
    y = y_raw
    y_pred = loaded_clf.predict(X)
    print(confusion_matrix(y, y_pred))


def main():
    if len(sys.argv) != 5:
        usage()

    for arg in sys.argv:
        print("Arg: {}".format(arg))

    print("Will train SVM. Assuming every training data point has {0} "
          "features.".format(FEATURE_COUNT))
    csv_file = sys.argv[1]
    dest_pickle_file_prefix = sys.argv[2]
    C = float(sys.argv[3])
    probabilistic = sys.argv[4] == "1"

    # A simple linear (for the time being) SVM classifier using the optimal
    # parameters established via grid search in the notebook.
    loss = 'log'
    penalty = 'l1'
    n_iter = 5
    alpha = 0.0005
    clf = SGDClassifier(class_weight='balanced', loss=loss, penalty=penalty,
                        n_iter=n_iter, alpha=alpha)
    dest_pickle_file = "{}-sgd-loss-{}-pen-{}-niter-{}-alpha-{}.pkl".format(
        dest_pickle_file_prefix, loss, penalty, n_iter, alpha)
    print("Ignoring C parameter and training using SGD.")

    # The non-linear version. Much more expensive to train, but yields somewhat
    # better results, and corresponds to what is described in the paper.
    dest_pickle_file = "{0}-svc-c-{1:6.4f}.pkl".format(dest_pickle_file_prefix, C)
    clf = SVC(C=C, class_weight='balanced', probability=probabilistic)

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


if __name__ == '__main__':
    main()
