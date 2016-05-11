from flask import Flask, request
app = Flask(__name__)

#app.config['DEBUG'] = True
SEPARATOR = ","

@app.route("/")
def hello():
    return "Move along, nothing to see here."

@app.route("/predict", methods=['GET', 'POST'])
def predict():
	features_string = request.args.get("features_string")
	features = [float(x) for x in features_string.split(SEPARATOR)]

	s = sum(features) # TODO this is where the magic (classification) needs to happen

	return(str(s))

if __name__ == "__main__":
    app.run()