#Clase modelo para una red neuronal 
import numpy as np

class NeuralNetwork(object):
    def __init__(self):
        #Se definen las neuronas de cada capa
        self.inputLayerSize = 5
        self.outputLayerSize = 2
        self.hiddenLayerSize = 9
    
        #Se definen valores random para los pesos iniciales
        self.w1 = np.random.rand(self.inputLayerSize, self.hiddenLayerSize)
        self.w2 = np.random.rand(self.hiddenLayerSize, self.outputLayerSize)
    
    def forward(self, s):
        #Normalizacion de los valores de los sensores
        s = s/np.amax(s, axis = 0)
        #Se propagan las entradas por la red hasta las salidas
        self.z2 = np.dot(s, self.w1)
        self.a = self.sigmoid(self.z2)
        self.z3 = np.dot(self.a, self.w2)
        self.mHat = self.sigmoid(self.z3)
        return self.mHat
        
    def sigmoid(self, z):
        #Funcion de activacion sigmoid
        return 1/(1 + np.exp(-z))
        
    def sigmoidPrime(self, z):
        #Derivada de la funcion sigmoid
        return np.exp(-z)/((1 + np.exp(-z))**2)
        
    def costFunction(self, s, m):
        #Calcular el error para los valores de los sensores y los
        #motores dados, se usan los pesos guardados en la clase.
        self.mHat = self.forward(s)
        e = 0.5*sum((m-self.mHat)**2)
        return e
    
    def costFunctionPrime(self, s, m):
        #Calcula la derivada con respecto a w1 y w2
        self.mHat = self.forward(s)
        
        delta3 = np.multiply(-(m-self.mHat), self.sigmoidPrime(self.z3))
        djdw2 = np.dot(self.a.T, delta3)
        
        delta2 = np.dot(delta3, self.w2.T) * self.sigmoidPrime(self.z2)
        djdw1 = np.dot(s.T, delta2)
        return djdw1, djdw2
        
    #funciones para interactuar con otras clases:
    def getParams(self):
        #obtiene w1 y w2 como un vector:
        params = np.concatenate((self.W1.ravel(), self.W2.ravel()))
        return params
    
    def setParams(self, params):
        #asigna w1 y w2 
        W1_start = 0
        W1_end = self.hiddenLayerSize * self.inputLayerSize
        self.w1 = np.reshape(params[W1_start:W1_end], (self.inputLayerSize , self.hiddenLayerSize))
        W2_end = W1_end + self.hiddenLayerSize*self.outputLayerSize
        self.w2 = np.reshape(params[W1_end:W2_end], (self.hiddenLayerSize, self.outputLayerSize))
        
    def computeGradients(self, s, m):
        dJdw1, dJdw2 = self.costFunctionPrime(s, m)
        return np.concatenate((dJdw1.ravel(), dJdw2.ravel()))

    def computeNumericalGradient(N, s, m):
        paramsInitial = N.getParams()
        numgrad = np.zeros(paramsInitial.shape)
        perturb = np.zeros(paramsInitial.shape)
        e = 1e-4

        for p in range(len(paramsInitial)):
            #Set perturbation vector
            perturb[p] = e
            N.setParams(paramsInitial + perturb)
            loss2 = N.costFunction(s, m)
            
            N.setParams(paramsInitial - perturb)
            loss1 = N.costFunction(s, m)

            #Compute Numerical Gradient
            numgrad[p] = (loss2 - loss1) / (2*e)

            #Return the value we changed to zero:
            perturb[p] = 0
            
        #Return Params to original value:
        N.setParams(paramsInitial)

        return numgrad

from scipy import optimize


class trainer(object):
    def __init__(self, N):
        #Make Local reference to network:
        self.N = N
        
    def callbackF(self, params):
        self.N.setParams(params)
        self.E.append(self.N.costFunction(self.s, self.m))   
        
    def costFunctionWrapper(self, params, s, m):
        self.N.setParams(params)
        cost = self.N.costFunction(s, m)
        grad = self.N.computeGradients(s,m)
        return cost, grad
        
    def train(self, s, m):
        #Make an internal variable for the callback function:
        self.s = s
        self.m = m

        #Make empty list to store costs:
        self.E = []
        
        params0 = self.N.getParams()

        options = {'maxiter': 200, 'disp' : True}
        _res = optimize.minimize(self.costFunctionWrapper, params0, jac=True, method='BFGS', \
                                 args=(s, m), options=options, callback=self.callbackF)

        self.N.setParams(_res.x)
        self.optimizationResults = _res
    
    