from __future__ import print_function

import pickle
import sys

from flask import Flask, request
import numpy as np
from sklearn.linear_model import SGDClassifier
from sklearn.svm import SVC

app = Flask(__name__)
classifier = None

@app.route("/")
def hello():
    return "Move along, nothing to see here."


@app.route("/predict", methods=['GET', 'POST'])
def predict():
    features_string = request.args.get("features_string")
    features = [float(x) for x in features_string.split(SEPARATOR)]

    # Ensure that our data
    x = np.array(features).reshape((1, -1))
    try:
        s = classifier.predict(x)
    except ValueError as err:
        print("Predict exception:", err, file=sys.stderr)
    except:
        print("Unexpected exception.", sys.exc_info()[0], file=sys.stderr)

    return str(s[0])


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: server.py <port> <input_separator> <model_pickle_path>")
        exit(1)

    PORT		= int(sys.argv[1])
    SEPARATOR	= sys.argv[2]
    MODEL_PATH  = sys.argv[3]

    # TODO(andrei): Remember and apply scaling parameters as well!
    global classifier
    classifier = pickle.load(open(MODEL_PATH, 'rb'))

    app.config['DEBUG'] = True
    app.run(port=PORT)