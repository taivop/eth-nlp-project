from flask import Flask, request
import sys
app = Flask(__name__)


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
	PORT		= int(sys.argv[1])
	SEPARATOR	= sys.argv[2]

	#app.config['DEBUG'] = True
	app.run(port=PORT)