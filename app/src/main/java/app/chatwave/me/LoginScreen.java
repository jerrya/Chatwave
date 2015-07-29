package app.chatwave.me;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class LoginScreen extends Fragment {

    private static final String TAG = "LoginScreen";

    private Button loginButton, registerButton;

    public static ConnectionManager connectionManager = new ConnectionManager();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_screen_layout, container, false);

        loginButton = (Button) view.findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.instance().sendTracker("LoginScreen", "Login button", "Clicked login button", "Button click");

                AskLogin askLogin = new AskLogin();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, askLogin, "asklogin");
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        registerButton = (Button) view.findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.instance().sendTracker("LoginScreen", "Register button", "Clicked register button", "Button click");
                CreateAccount createAccount = new CreateAccount();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, createAccount, "createaccount");
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        return view;
    }
}
