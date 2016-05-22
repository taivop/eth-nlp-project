"""Utilities for loading/preparing feature data for use in a classifier."""

from __future__ import print_function

import numpy as np
#from sklearn import preprocessing

from sklearn.preprocessing import StandardScaler

from sklearn.decomposition import PCA

import sklearn

# pylint: disable=invalid-name
# Disable spurious error messages (known issue with syntastic + pylint + np).
# pylint: disable=no-member

def load_training_data(csv_file_name, feature_count):
    """Loads a CSV file produced by the Java feature generator."""

    X_raw = []
    y_raw = []
    with open(csv_file_name, 'r') as f:
        bad_lines = 0
        for line_number, line in enumerate(f.readlines()):
            if bad_lines > 1000:
                print("Too many bad lines. The parsing code is bugged or the "
                      "CSV is badly formatted. Get yo shit together fam.")
                break

            parts = line[:-1].split(",")
            if len(parts) != 8 + feature_count:
                bad_lines += 1
                # print("Skipping bad line")
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

        # print("Feature shape: {0}".format(X_raw.shape))
        # print("Label shape: {0}".format(y_raw.shape))

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
    # print(np.mean(X, axis=1))
    # print(np.std(X, axis=1))

    # No scaling needed for y.
    return X, y, ranges, means, scaler

def split_dataset(X_rescaled,y_rescaled,neg_to_pos_ratio,valid_set_ratio):
    # X, y = sklearn.utils.shuffle(X_rescaled, y_rescaled)

    pos_count = np.sum(y_rescaled == 1)
    neg_count = np.sum(y_rescaled == 0)

    # print("We have {0} positive labels.".format(pos_count))
    # print("We have {0} negative labels.".format(neg_count))

    '''
    # Use a much smaller ratio of negative to positive samples in the validation
    # set, for more accurate validation results.

    pos_count_valid = int(pos_count * 0.15)
    neg_count_valid = pos_count_valid * neg_to_pos_ratio

    # Indexes of positive rows to use for validation.
    # This witchcraft isolates the indexes of the first 'pos_count_valid' rows
    # with positive labels in the training data.
    y_pos_ind = (y == 1)
    y_pos_counts = np.cumsum(y_pos_ind)
    y_pos_lim = np.where(y_pos_counts == (pos_count_valid + 1))[0][0]
    y_pos_ind[y_pos_lim:] = False

    # This bit does the same but for the first 'neg_count_valid' rows with negative
    # labels.
    y_neg_ind = (y == 0)
    y_neg_counts = np.cumsum(y_neg_ind)
    y_neg_lim = np.where(y_neg_counts == (neg_count_valid + 1))[0][0]
    y_neg_ind[y_neg_lim:] = False

    # Make sure that there's no overlap, which would signify that we messed something
    # up with the slicing/indexing.
    assert np.sum(y_pos_ind & y_neg_ind) == 0

    X_valid = X[y_pos_ind | y_neg_ind]
    y_valid = y[y_pos_ind | y_neg_ind]

    X_train = X[~(y_pos_ind | y_neg_ind)]
    y_train = y[~(y_pos_ind | y_neg_ind)]

    # Just some manual extra checks.
    # TODO(andrei): Label better or remove.
    '''

    pos_ids = np.where(y_rescaled==1)[0]
    neg_ids = np.where(y_rescaled==0)[0]

    pos_count_valid = int(round(pos_count * valid_set_ratio))
    neg_count_valid = int(round(neg_count * valid_set_ratio))

    pos_ids_valid = np.random.choice(pos_ids,size=pos_count_valid,replace=False)
    neg_ids_valid = np.random.choice(neg_ids,size=neg_count_valid,replace=False)

    pos_ids_valid_idx = list(map(lambda x : np.where(pos_ids == x),pos_ids_valid))
    neg_ids_valid_idx = list(map(lambda x : np.where(neg_ids == x),neg_ids_valid))

    pos_ids_train = np.delete(pos_ids,pos_ids_valid_idx)
    neg_ids_train = np.delete(neg_ids,neg_ids_valid_idx)

    # oversample positive samples so that neg_to_pos_ratio is met
    # reps = int(np.ceil(neg_count / (pos_count * neg_to_pos_ratio)))
    # pos_ids_valid = np.tile(pos_ids_valid,reps=[reps])

    X_train_pos = X_rescaled[pos_ids_train,:]
    X_train_neg = X_rescaled[neg_ids_train,:]
    X_train = np.append(X_train_pos,X_train_neg,axis=0)

    y_train_pos = y_rescaled[pos_ids_train]
    y_train_neg = y_rescaled[neg_ids_train]
    y_train = np.append(y_train_pos,y_train_neg)

    y_train_exp = np.expand_dims(y_train,axis=1)

    train_set = np.append(y_train_exp,X_train,axis=1)
    np.random.shuffle(train_set)

    X_train = train_set[:,1:]
    y_train = train_set[:,0]

    X_valid_pos = X_rescaled[pos_ids_valid,:]
    X_valid_neg = X_rescaled[neg_ids_valid,:]
    X_valid = np.append(X_valid_pos,X_valid_neg,axis=0)

    y_valid_pos = y_rescaled[pos_ids_valid]
    y_valid_neg = y_rescaled[neg_ids_valid]

    y_valid = np.append(y_valid_pos,y_valid_neg)

    y_valid_exp = np.expand_dims(y_valid,axis=1)

    valid_set = np.append(y_valid_exp,X_valid,axis=1)
    np.random.shuffle(valid_set)

    X_valid = valid_set[:,1:]
    y_valid = valid_set[:,0]

    '''
    print("Training X shape : " + str(X_train.shape) + " y shape : " + str(y_train.shape))
    print("Validation X shape = : " + str(X_valid.shape) + " y shape : " + str(y_valid.shape))

    print("Positive = " + str(np.sum(y_valid==1)) + " Negative = " + str(np.sum(y_valid==0)))
    '''

    return X_train, y_train, X_valid, y_valid

def load_dataset(csv_file_name,feature_count,neg_to_pos_ratio,valid_set_ratio):
    X_raw, Y_raw = load_training_data(csv_file_name=csv_file_name,feature_count=feature_count)
    X_rescaled, y_rescaled, _, _, _ = rescale(X=X_raw, y=Y_raw)
    X_train, y_train, X_valid, y_valid = split_dataset(X_rescaled=X_rescaled,
                                                       y_rescaled=y_rescaled,
                                                       neg_to_pos_ratio=neg_to_pos_ratio,
                                                       valid_set_ratio=valid_set_ratio)

    return X_train, y_train, X_valid, y_valid

def pca_components(x,n_components):
    x_t = x.T

    pca = PCA(n_components=n_components)
    pca.fit(x_t)

    pca_components_t = pca.components_

    pca_components = pca_components_t.T

    for i in range(n_components):
        explained_variance = np.sum(pca.explained_variance_ratio_[:(i+1)])
        # print("Principal components = " + str(i+1) + " Explained variance = " + str(explained_variance))

    return pca_components

def load_dataset_pca(csv_file_name,feature_count,neg_to_pos_ratio,valid_set_ratio,n_components):
    X_raw, Y_raw = load_training_data(csv_file_name=csv_file_name,feature_count=feature_count)
    X_rescaled, y_rescaled, _, _, _ = rescale(X=X_raw, y=Y_raw)

    X_rescaled_pca = pca_components(X_rescaled,n_components=n_components)

    X_train, y_train, X_valid, y_valid = split_dataset(X_rescaled=X_rescaled_pca,
                                                       y_rescaled=y_rescaled,
                                                       neg_to_pos_ratio=neg_to_pos_ratio,
                                                       valid_set_ratio=valid_set_ratio)

    return X_train, y_train, X_valid, y_valid

if __name__ ==  "__main__":
    X_raw, Y_raw = load_training_data(csv_file_name="../../../data/all-candidates-5-18-15-13.csv",feature_count=20)
    X_rescaled, y_rescaled, _, _, _ = rescale(X=X_raw, y=Y_raw)
    X_train, y_train, X_valid, y_valid = split_dataset(X_rescaled=X_rescaled, y_rescaled=y_rescaled,neg_to_pos_ratio=1,valid_set_ratio=0.1)




