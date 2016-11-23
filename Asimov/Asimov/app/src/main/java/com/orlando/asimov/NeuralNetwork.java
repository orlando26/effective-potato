package com.orlando.asimov;

import java.util.Random;

/**
 * Created by orlando on 21/11/16.
 */
public class NeuralNetwork {
    private int inputLayerSize = 3;
    private int hiddenLayerSize = 5;
    private int outputLayerSize = 1;

    private double[][] W1 = new double[inputLayerSize][hiddenLayerSize];
    private double[][] W2 = new double[hiddenLayerSize][outputLayerSize];

    public NeuralNetwork(){
        setRandomWeights();
    }

    /* NN Methods*/

    public double normalization(double in, double outMin, double outMax){
        double normalized;

        normalized = (in-outMin)/((outMax-outMin) + 0.000001);
        if(normalized > 1){
            normalized = 1;
        }
        if(normalized < 0){
            normalized = 0;
        }
        return normalized;
    }

    public double desnormalization(double in, double outMin, double outMax){
        double desnormalized;
        desnormalized = in*outMax + outMin*(1 - in);

        if(desnormalized > outMax){
            desnormalized = outMax;
        }
        if(desnormalized < outMin){
            desnormalized = outMin;
        }
        return desnormalized;
    }

    /**
     * sets random values to the weight arrays
     */
    public void setRandomWeights(){
        Random rnd = new Random();
        for(int i = 0;i < inputLayerSize;i++){
            for(int j = 0; j < hiddenLayerSize;j++){
                W1[i][j] = rnd.nextDouble();
            }
        }

        for(int i = 0;i < hiddenLayerSize;i++){
            for(int j = 0; j < outputLayerSize;j++){
                W2[i][j] = rnd.nextDouble();
            }
        }
    }

    /**
     * uses the forward propagation method
     * using the current weights the Neural Network has
     * to predict the output for a given input
     * @param x1
     * @param x2
     * @return yHat
     */
    public double[][] forward(double x1, double x2){
        x1 = normalization(x1, 74, 101);
        x2 = normalization(x2, 77, 97);
        double[][] X = {{x1, x2, 1}};

        double[][] SUM = muliply(X, W1);
        double[][] N = activationFunction(SUM);

        double[][] SUM2 = muliply(N, W2);
        double[][] yHat = activationFunction(SUM2);

        yHat[0][0] = desnormalization(yHat[0][0], 0, 90);
        return yHat;
    }

    /**
     * return the product operation of two matrix arrays
     * @param A matrix array
     * @param B matrix array
     * @return A * B
     */
    public double[][] muliply(double[][] A, double[][] B) {
        int aRows = A.length;
        int aColumns = A[0].length;
        int bRows = B.length;
        int bColumns = B[0].length;

        if (aColumns != bRows) {
            throw new IllegalArgumentException("A:Rows: " + aColumns + " did not match B:Columns " + bRows + ".");
        }

        double[][] C = new double[aRows][bColumns];
        for (int i = 0; i < aRows; i++) {
            for (int j = 0; j < bColumns; j++) {
                C[i][j] = 0.00000;
            }
        }

        for (int i = 0; i < aRows; i++) { // aRow
            for (int j = 0; j < bColumns; j++) { // bColumn
                for (int k = 0; k < aColumns; k++) { // aColumn
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }

        return C;
    }

    /**
     * sigmoid function
     * @param x
     * @return 1/(1 + e^-x)
     */
    public double sigmoid(double x){
        return 1/(1 + Math.exp(-x));
    }

    /**
     * aplies the activation function to each value in an array
     * @param val
     * @return f(val)
     */
    public double[][] activationFunction(double[][] val){
        for(int i = 0;i < val.length;i++){
            for(int j = 0;j < val[0].length;j++){
                val[i][j] = sigmoid(val[i][j]);
            }
        }
        return val;
    }

    /*Getters and Setter*/
    public int getInputLayerSize() {
        return inputLayerSize;
    }

    public void setInputLayerSize(int inputLayerSize) {
        this.inputLayerSize = inputLayerSize;
    }

    public int getHiddenLayerSize() {
        return hiddenLayerSize;
    }

    public void setHiddenLayerSize(int hiddenLayerSize) {
        this.hiddenLayerSize = hiddenLayerSize;
    }

    public int getOutputLayerSize() {
        return outputLayerSize;
    }

    public void setOutputLayerSize(int outputLayerSize) {
        this.outputLayerSize = outputLayerSize;
    }

    public double[][] getW1() {
        return W1;
    }

    public void setW1(double[][] W1) {
        this.W1 = W1;
    }

    public double[][] getW2() {
        return W2;
    }

    public void setW2(double[][] W2) {
        this.W2 = W2;
    }

    /**
     * returns a readable string of an array
     * @param array
     * @return arrayString
     */
    public String get(double[][] array){
        String arrayString = "";

        for(int i = 0;i < array.length;i++){
            for(int j = 0;j < array[0].length;j++){
                arrayString += String.format("%.2f ", array[i][j]);
            }
            arrayString += "\n";
        }
        return arrayString;
    }
}
