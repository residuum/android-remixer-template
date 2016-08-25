package residuum.org.remixer.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by thomas on 25.08.16.
 */

public class LogSeekBar extends SeekBar {
    private final static int SLIDER_RANGE = 1000; // number of discrete steps in native widget.
    private double curReal, minReal, maxReal;
    private boolean isLogScale;
    private Set<ChangeListener> listeners = new HashSet<ChangeListener>();
    public interface ChangeListener {
        public void onChange(double newValue);
    }

    public LogSeekBar(Context context, double cur, double min, double max) {
        super(context);
        init(cur, min, max, false);
    }
    public LogSeekBar(Context context) {
        this(context, 0, 0, SLIDER_RANGE);
    }

    public LogSeekBar(Context context, AttributeSet attrs, double cur, double min, double max) {
        super(context, attrs);
        init(cur, min, max, false);
    }
    public LogSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0, SLIDER_RANGE);
    }

    public LogSeekBar(Context context, AttributeSet attrs, int defStyle, double cur, double min, double max) {
        super(context, attrs, defStyle);
        init(cur, min, max, false);
    }
    public LogSeekBar(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0, 0, SLIDER_RANGE);
    }

    /**
     * @param cur - real valued initial value.
     * @param min - real valued range minimum.
     * @param max - real valued range maximum.
     * @param log - log scale if true, linear otherwise.
     */
    private void init(double cur, double min, double max, boolean log) {
        isLogScale = log;
        setAll(min, max, cur, log);
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int ival, boolean fromUser) {
                double dval = transformRange(
                        false, 0, SLIDER_RANGE, ival,
                        isLogScale, minReal, maxReal
                );
                setRealValue(dval);
                // Notify all listeners.
                fireChangeEvent();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        super.setOnSeekBarChangeListener(listener);
    } // end init()

    //
    // LISTENER SUPPORT
    //

    public void addListener(ChangeListener l) {
        listeners.add(l);
    }

    public boolean removeListener(ChangeListener l) {
        return listeners.remove(l);
    }

    protected void fireChangeEvent() {
        for (ChangeListener l : listeners)
            l.onChange(LogSeekBar.this.getRealValue());
    }

    //
    // GETTERS & SETTERS
    //

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        // We can't allow users to set the base class' listener both because this class needs to do that
        // and because the values it returns make no sense outside of this class.
        throw new UnsupportedOperationException ("Use addListener() instead");
    }

    public double getRealMinimum() {
        return minReal;
    }

    public void setRealMinimum(double newmin) {
        setAll(newmin, maxReal, curReal, isLogScale);
    }

    public double getRealMaximum() {
        return maxReal;
    }

    public void setRealMaximum(double newmax) {
        setAll(minReal, newmax, curReal, isLogScale);
    }

    public double getRealValue() {
        return curReal;
    }

    public boolean getLog() {
        return isLogScale;
    }

    public void setLogScale(boolean isLogScale) {
        setAll(minReal, maxReal, curReal, isLogScale);
    }

    public void setRealValue(double newcur) {
        // update the model
        newcur = Math.max(newcur, minReal);
        newcur = Math.min(newcur, maxReal);
        setAll(minReal, maxReal, newcur, isLogScale);
    }

    public void setAll(double newmin, double newmax, double newcur, boolean log) {
        minReal = newmin;
        maxReal = newmax;
        curReal = newcur;
        isLogScale = log;
        // update the view
        int imax = rangeValue(newmax);
        super.setMax(imax);
        int icur = rangeValue(newcur);
        super.setProgress(icur);
    }

    //
    // MODEL IMPLEMENTATION
    //

    private static double clamp(double x, double a, double b) {
        return x <= a ? a :
                x >= b ? b : x;
    }

    // linear interpolation
    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // geometric interpolation
    private static double gerp(double a, double b, double t) {
        return a * Math.pow(b / a, t);
    }

    // interpolate between A and B (linearly or geometrically)
    // by the fraction that x is between a and b (linearly or geometrically)
    private static double transformRange(
            boolean isLog, double a, double b, double x,
            boolean IsLog, double A, double B) {
        if (isLog) {
            a = Math.log(a);
            b = Math.log(b);
            x = Math.log(x);
        }
        double t = (x - a) / (b - a);
        double X = IsLog ?
                gerp(A, B, t) :
                lerp(A, B, t);
        return X;
    }

    /**
     * @return the closest integer in the range of the actual int extents of the base class.
     */
    protected int rangeValue(double dval) {
        dval = clamp(dval, minReal, maxReal);
        int ival = (int) Math.round(
                transformRange(isLogScale, minReal, maxReal, dval, false, 0, SLIDER_RANGE));
        return ival;
    }

}

