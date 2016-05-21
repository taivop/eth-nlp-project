"""Utilities for loading/preparing feature data for use in a classifier."""

from __future__ import print_function

import numpy as np
from sklearn import preprocessing
from sklearn.preprocessing import StandardScaler


# pylint: disable=invalid-name
# Disable spurious error messages (known issue with syntastic + pylint + np).
# pylint: disable=no-member

def load_training_data(csv_file_name, feature_count):
    """Loads a CSV file produced by the Java feature generator."""

    X_raw = []
    y_raw = []
    expected_part_count = 8 + feature_count
    with open(csv_file_name, 'r') as f:
        bad_lines = 0
        for line_number, line in enumerate(f.readlines()):
            if bad_lines > 10:
                print("Too many bad lines. The parsing code is bugged or the "
                      "CSV is badly formatted. Get yo shit together fam.")
                break

            parts = line[:-1].split(",")
            if len(parts) != expected_part_count:
                bad_lines += 1
                print("Skipping bad line: {}".format(line))
                print("Expected {} but found {} columns.".format(
                    expected_part_count, len(parts)))
                continue

            # Example CSV line, as of May 10.
            # 364646, lumet familt, 7, 19, 12, featureStart, [feature_count features],
            # featureEnd, true
            try:
                # Not used at the moment.
                # meta = parts[:5]
                features = parts[6:6 + feature_count]
                label = parts[-1]
                features_np = np.array([float(f) for f in features])
                X_raw.append(features_np)
                y_raw.append(1 if label == 'true' else 0)
            except ValueError as e:
                print("Could not parse data line {0}: {1}.".format(line_number, line))
                print(e)
                bad_lines += 1

        X_raw = np.array(X_raw)
        y_raw = np.array(y_raw)

        if bad_lines > 0:
            print("Bad lines: {0}".format(bad_lines))
        else:
            print("Import was successful.")

        print("Feature shape: {0}".format(X_raw.shape))
        print("Label shape: {0}".format(y_raw.shape))

        X_raw = impute_nan_inf(X_raw)
        return X_raw, y_raw


def impute_nan_inf(X_raw):
    """Hacky imputation of NaNs and Infs."""

    X_raw[np.isnan(X_raw)] = 0.0
    # TODO(andrei): Ensure that this makes sense.
    X_raw[np.isinf(X_raw)] = 10000.0
    return X_raw


def rescale(X, y):
    """Rescales the data, in preparation for the SVM training.

    Only operates on X, since there's no scaling to perform on labels.
    Performs the rescaling in two phases, a manual one, and one which employs
    a 'sklearn.StandardScaler'. This is necessary in order to prevent sklearn's
    scaler from warning about excessive data ranges.

    Returns:
        The rescaled X, y, as well as the ranges and means of X's features,and
        the pre-fit sklearn scaler.
    """
    ranges = np.max(X, axis=0) - np.min(X, axis=0)
    means = np.mean(X, axis=0)
    # Avoid divisions by zero
    ranges[ranges == 0] = 1.0
    X = (X - means) / ranges
    scaler = StandardScaler()
    X = scaler.fit_transform(X)
    print(np.mean(X, axis=1))
    print(np.std(X, axis=1))

    # No scaling needed for y.
    return X, y, ranges, means, scaler

