package app.chatwave.me;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import app.chatwave.me.EventPackage.MessageEvent;
import de.greenrobot.event.EventBus;

public class AskLogin extends Fragment {

    private EditText login_username, login_password;
    private Button login_button, login_back;

    private String blockCharacterSet = "@)(~#^|$%&*!/";
    private InputFilter inputFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if(source != null && blockCharacterSet.contains(("" + source))) {
                return "";
            }
            return null;
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ask_login_layout, container, false);

        login_username = (EditText) view.findViewById(R.id.login_username);
        login_username.setFilters(new InputFilter[]{inputFilter, new InputFilter.LengthFilter(20)});

        login_password = (EditText) view.findViewById(R.id.login_password);

        login_button = (Button) view.findViewById(R.id.login_button);
        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = login_username.getText().toString().trim();
                String password = login_password.getText().toString().trim();
                if(username.length() > 0 && password.length() > 0) {
                    Intent mServiceIntent = new Intent(getActivity(), ConnectionManager.class);
                    mServiceIntent.putExtra("event", 0);
                    mServiceIntent.putExtra("username", username);
                    mServiceIntent.putExtra("password", password);
                    getActivity().startService(mServiceIntent);
                }
            }
        });

        login_back = (Button) view.findViewById(R.id.login_back);
        login_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onEventMainThread(final MessageEvent event){
        if (event.successful) {
            startMainFragment();
        } else {
            Toast.makeText(getActivity(), "Unable to login", Toast.LENGTH_LONG).show();
        }
    }

    public void startMainFragment() {
        if (getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        MainScreen mainScreen = new MainScreen();
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, mainScreen, "mainscreen");
        transaction.commit();
    }
}
