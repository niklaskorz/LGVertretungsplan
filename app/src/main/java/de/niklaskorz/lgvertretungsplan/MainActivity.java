package de.niklaskorz.lgvertretungsplan;

import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.os.Build;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import java.text.SimpleDateFormat;


public class MainActivity extends ActionBarActivity implements PlanClient.ResponseHandler {
    Toolbar toolbar;
    PlanClient planClient;
    LoginManager loginManager;
    SwipeRefreshLayout swipeRefreshLayout;
    PlanClient.Type lastPlanType = PlanClient.Type.TODAY;

    RecyclerView recyclerView;
    RecyclerView.Adapter adapter;
    LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.primaryDark));
            window.setNavigationBarColor(getResources().getColor(R.color.primaryDark));
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setHomeButtonEnabled(true);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadPlan(lastPlanType);
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.red, R.color.green, R.color.blue);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        planClient = new PlanClient(this);

        swipeRefreshLayout.setRefreshing(true);
        loginManager = new LoginManager(this, planClient);
        loginManager.login(lastPlanType, this);
    }

    public void loadPlan(final PlanClient.Type type) {
        lastPlanType = type;
        swipeRefreshLayout.setRefreshing(true);
        planClient.get(type, this);
    }

    @Override
    public void onSuccess(Plan p) {
        swipeRefreshLayout.setRefreshing(false);
        adapter = new PlanAdapter(layoutManager, p);
        recyclerView.setAdapter(adapter);
        SnackbarManager.show(Snackbar
                .with(this)
                .text(p.lastUpdateString)
                .duration(Snackbar.SnackbarDuration.LENGTH_LONG));

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd.MM.yyyy");
        setTitle(dateFormat.format(p.date));
    }

    @Override
    public void onFailure(final Throwable error) {
        final Context context = this;

        swipeRefreshLayout.setRefreshing(false);
        error.printStackTrace();
        SnackbarManager.show(Snackbar
                .with(this)
                .text("Vertretungsplan konnte nicht geladen werden")
                .actionLabel("Info")
                .actionListener(new ActionClickListener() {
                    @Override
                    public void onActionClicked(Snackbar snackbar) {
                        new MaterialDialog.Builder(context)
                                .title("Something went wrong")
                                .content(error.getLocalizedMessage())
                                .positiveText("Close")
                                .show();
                    }
                })
                .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("username", loginManager.getUsername());
            intent.putExtra("fullname", loginManager.getUserFullname());
            startActivity(intent);
            return true;
        } else if (id == R.id.action_signout) {
            loginManager.logout();
            recyclerView.setAdapter(null);
            loginManager.login(lastPlanType, this);
        } else if (id == R.id.action_today) {
            loadPlan(PlanClient.Type.TODAY);
        } else if (id == R.id.action_tomorrow) {
            loadPlan(PlanClient.Type.TOMORROW);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
