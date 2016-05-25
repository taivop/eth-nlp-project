from __future__ import print_function

import pickle
import sys

from flask import Flask, request
import numpy as np
from sklearn.linear_model import SGDClassifier
from sklearn.svm import SVC

from data_util import impute_nan_inf

app = Flask(__name__)

# The parameters we will be loading from the pickle.
classifier = None
magnitudes = None
means = None
scaler = None


@app.route("/")
def hello():
    return "Move along, nothing to see here."


@app.route("/predict", methods=['GET', 'POST'])
def predict():
    try:
        features_string = request.args.get("features_string")
        probabilistic = int(request.args.get("probabilistic"))
        features = [float(x) for x in features_string.split(SEPARATOR)]

        # Ensure that our data is a one row matrix, as expected by sklearn.
        x = np.array(features).reshape((1, -1))
        # Perform the scaling.
        x = impute_nan_inf(x)
        x = scaler.transform((x - means) / magnitudes)
        try:
            if probabilistic:
                s = classifier.predict_proba(x)
            else:
                s = classifier.predict(x)
        except ValueError as err:
            print("Predict exception:", err, file=sys.stderr)

        return str(s[0])
    except:
        print("Unexpected exception in Python API.", sys.exc_info()[0], file=sys.stderr)


def shutdown_server():
    func = request.environ.get('werkzeug.server.shutdown')
    if func is None:
        raise RuntimeError('Not running with the Werkzeug Server')

    print("Shutting down Flask API.", file=sys.stderr)
    print("I was using the following classifier: {}".format(classifier), file=sys.stderr)
    func()

if __name__ == "__main__":
    global classifier, magnitudes, means, scaler

    if len(sys.argv) != 4:
        print("Usage: server.py <port> <input_separator> <model_pickle_path>")
        exit(1)

    PORT		= int(sys.argv[1])
    SEPARATOR	= sys.argv[2]
    MODEL_PATH  = sys.argv[3]

    classifier, magnitudes, means, scaler = pickle.load(open(MODEL_PATH, 'rb'))
    print("\n\nStarting server.")
    print("Classifier loaded from: {}".format(MODEL_PATH))
    print("Classifier: {}".format(classifier))
    sys.stdout.flush()

    app.config['DEBUG'] = True
    app.run(port=PORT)
