package org.multidisciplinary;

/**
 * A simple complex number class to support FFT operations.
 * Supports addition, subtraction, multiplication, magnitude calculation, and exponential functions.
 */
public final class Complex {

    private final double re; // Real part
    private final double im; // Imaginary part

    /**
     * Creates a new complex number.
     *
     * @param real The real part of the complex number.
     * @param imag The imaginary part of the complex number.
     */
    public Complex(double real, double imag) {
        this.re = real;
        this.im = imag;
    }

    /**
     * Returns the magnitude (absolute value) of this complex number.
     *
     * @return The magnitude of the complex number.
     */
    public double abs() {
        return Math.hypot(re, im);
    }

    /**
     * Adds this complex number to another.
     *
     * @param b The complex number to add.
     * @return A new complex number representing the sum.
     */
    public Complex plus(Complex b) {
        return new Complex(this.re + b.re, this.im + b.im);
    }

    /**
     * Subtracts another complex number from this one.
     *
     * @param b The complex number to subtract.
     * @return A new complex number representing the difference.
     */
    public Complex minus(Complex b) {
        return new Complex(this.re - b.re, this.im - b.im);
    }

    /**
     * Multiplies this complex number by another.
     *
     * @param b The complex number to multiply by.
     * @return A new complex number representing the product.
     */
    public Complex times(Complex b) {
        final double real = this.re * b.re - this.im * b.im;
        final double imag = this.re * b.im + this.im * b.re;
        return new Complex(real, imag);
    }

    /**
     * Returns a new complex number that is the conjugate of this one.
     *
     * @return The conjugate of this complex number.
     */
    public Complex conjugate() {
        return new Complex(re, -im);
    }

    /**
     * Returns the complex number as a string in the format "a + bi".
     *
     * @return A string representation of the complex number.
     */
    @Override
    public String toString() {
        return String.format("%.4f + %.4fi", re, im);
    }

    /**
     * Returns the real part of the complex number.
     *
     * @return The real part of the complex number.
     */
    public double getReal() {
        return re;
    }

    /**
     * Returns the imaginary part of the complex number.
     *
     * @return The imaginary part of the complex number.
     */
    public double getImaginary() {
        return im;
    }
}