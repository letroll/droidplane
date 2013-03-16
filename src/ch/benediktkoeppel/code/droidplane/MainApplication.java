package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;
import java.util.Stack;

import android.app.Application;
import android.net.Uri;
import android.widget.ListView;

import org.acra.*;
import org.acra.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@ReportsCrashes(formKey = "dE1VQVpQN2FNTWlLQXg1UUQ1b1VSN3c6MQ") 
public class MainApplication extends Application {
	
	public ArrayList<NodeColumn> nodeColumns;
	
	// the document which is used in MainActivity
	public Document document;

	private Uri uri;

	

	@Override
	public void onCreate() {
		super.onCreate();

		// initialize ACRA crash reports
		ACRA.init(this);
	}

	public Uri getUri() {
		return this.uri;
	}

	public void setUri(Uri uri) {
		this.uri = uri;
	}

	
	
}
