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

public class CreateAccount extends Fragment {

    private EditText create_username, create_password, create_email;
    private Button create_button, create_back;

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
        View view = inflater.inflate(R.layout.create_account_layout, container, false);

        create_username = (EditText) view.findViewById(R.id.create_username);
        create_username.setFilters(new InputFilter[]{inputFilter, new InputFilter.LengthFilter(20)});

        create_password = (EditText) view.findViewById(R.id.create_password);

        create_email = (EditText) view.findViewById(R.id.create_email);

        create_button = (Button) view.findViewById(R.id.create_button);
        create_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = create_username.getText().toString().trim();
                String password = create_password.getText().toString().trim();
                String email = create_email.getText().toString().trim();


                if(username.length() > 0) {
                    if(password.length() > 0) {
                        if(email.length() > 0 && email.contains("@")) {
                            Intent mServiceIntent = new Intent(getActivity(), ConnectionManager.class);
                            mServiceIntent.putExtra("event", 1);
                            mServiceIntent.putExtra("username", username);
                            mServiceIntent.putExtra("password", password);
                            mServiceIntent.putExtra("email", email);
                            getActivity().startService(mServiceIntent);
                        } else {
                            Toast.makeText(getActivity(), "Please enter a valid email", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Please enter a valid password", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), "Please enter a valid username", Toast.LENGTH_LONG).show();
                }
            }
        });

        create_back = (Button) view.findViewById(R.id.create_back);
        create_back.setOnClickListener(new View.OnClickListener() {
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
            if (getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            MainScreen mainScreen = new MainScreen();
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, mainScreen, "mainscreen");
            transaction.commit();
        } else {
            Toast.makeText(getActivity(), "Unable to login", Toast.LENGTH_LONG).show();
        }
    }
}
