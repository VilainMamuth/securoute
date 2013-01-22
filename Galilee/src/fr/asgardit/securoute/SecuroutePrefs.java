package fr.asgardit.securoute;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Button;

public class SecuroutePrefs extends PreferenceActivity {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add a button to the header list.
        if (hasHeaders()) {
            Button button = new Button(this);
            button.setText("Some action");
            setListFooter(button);
        }
    }
	
	
	
}
