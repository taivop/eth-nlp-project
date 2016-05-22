import threading
import numpy as np
from sklearn.metrics import confusion_matrix

class AdaBoostCVThread(threading.Thread):

    def __init__(self,
                 ada_boost_classifier,
                 x_train,
                 y_train,
                 x_test,
                 y_test,
                 pruning_threshold,
                 lock,
                 cm_total):

        threading.Thread.__init__(self)

        self.ada_boost_classifier = ada_boost_classifier

        self.x_train = x_train
        self.y_train = y_train
        self.x_test = x_test
        self.y_test = y_test

        self.pruning_threshold = pruning_threshold

        self.lock = lock

        self.cm_total = cm_total

    def ada_boost_cv(self):
        global cm_total

        self.ada_boost_classifier.fit(self.x_train,self.y_train)

        y_bar_proba = self.ada_boost_classifier.predict_proba(X=self.x_test)
        y_bar = np.array(y_bar_proba[:,1]>self.pruning_threshold,dtype=int)

        cm = confusion_matrix(self.y_test,y_bar)

        self.lock.acquire()
        self.cm_total += cm
        self.lock.release()