#!/usr/bin/env python
"""Utility to train and pickle a SMAPH classifier model."""

from __future__ import print_function

import pickle
import sys

import sklearn
from sklearn.linear_model import SGDClassifier
from sklearn.metrics import confusion_matrix
from sklearn.svm import SVC

from data_util import load_training_data, rescale

# pylint: disable=missing-docstring, invalid-name

# TODO(andrei): This should be in a common config.
FEATURE_COUNT = 10


def usage():
    print("Usage: train_smaph_model.py <csv_file> <dest_pickle_file>")
    exit(1)


def pickle_check(pickle_file, X, y):
    loaded_clf = pickle.load(pickle_file)
    y_pred = loaded_clf.predict(X)
    print(confusion_matrix(y, y_pred))


def main():
    if len(sys.argv) != 3:
        usage()

    # A simple linear (for the time being) SVM classifier using the optimal
    # parameters established via grid search in the notebook.
    # clf = SGDClassifier(class_weight='balanced', loss='hinge', penalty='l1',
                        # alpha=0.05)

    # The non-linear version. Much more expensive to train, but yields somewhat
    # better results, and corresponds to what is described in the paper.
    clf = SVC(C=1, class_weight='balanced')

    print("Will train SVM. Assuming every training data point has {0} "
          "features.".format(FEATURE_COUNT))
    csv_file = sys.argv[1]
    dest_pickle_file = sys.argv[2]
    print("Will read data from {0} and write the pickled model to "
          "{1}.".format(csv_file, dest_pickle_file))
    X_raw, y_raw = load_training_data(csv_file, FEATURE_COUNT)
    X, y = rescale(X_raw, y_raw)

    print("Read and processed {0} data points.".format(X.shape[0]))
    print("Will fit classifier:\n{0}".format(clf))

    clf.fit(X, y)

    print("Dumping pickle.")
    pickle.dump(clf, open(dest_pickle_file, 'wb'))

    print("Validating pickle.")
    pickle_check(open(dest_pickle_file, 'rb'), X, y)


if __name__ == '__main__':
    main()