package ir.android.persiantask.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import ir.android.persiantask.R;

public class TasksRepeatPeriodBottomSheetFragment extends BottomSheetDialogFragment {
    private View inlfateView;
    private Button repeatDayBtn;
    private NumberPicker numberPeriod;
    private NumberPicker typePeriod;
    private RepeatPeriodClickListener repeatTypeClickListener;
    private String[] typePeriodVal;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tasks_repeat_period, container,false);
        this.inlfateView = view;
        init();
        onClickListener();
        return view;
    }

    private void onClickListener() {
        repeatDayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                repeatTypeClickListener.onClickRepeatPeriod(getString(R.string.each) +
                        numberPeriod.getValue() + " " +
                        typePeriodVal[typePeriod.getValue()]);
            }
        });
    }

    private void init() {
        repeatDayBtn = inlfateView.findViewById(R.id.repeatPeriodBtn);
        numberPeriod = inlfateView.findViewById(R.id.numberPeriod);
        numberPeriod.setMinValue(2);
        numberPeriod.setMaxValue(100);
        typePeriod = inlfateView.findViewById(R.id.typePeriod);
        //@TODO get value from RepeatType enum
        typePeriodVal = new String[]{getString(R.string.day), getString(R.string.week),
                getString(R.string.month), getString(R.string.year)};
        typePeriod.setDisplayedValues(typePeriodVal);
        typePeriod.setMinValue(0);
        typePeriod.setMaxValue(3);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        repeatTypeClickListener = (RepeatPeriodClickListener) context;
    }

    public interface RepeatPeriodClickListener {
        void onClickRepeatPeriod(String repeatPeriod);
    }
}
