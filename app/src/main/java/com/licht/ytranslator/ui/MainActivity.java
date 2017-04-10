package com.licht.ytranslator.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.licht.ytranslator.R;
import com.licht.ytranslator.ui.HistoryView.HistoryListFragment;
import com.licht.ytranslator.ui.HistoryView.StarredListFragment;
import com.licht.ytranslator.ui.TranslateView.TranslateFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        Log.e("MainActivity", "setFragment(false, new TranslateFragment())");

        Fragment fmt = getSupportFragmentManager().findFragmentByTag(TAG);
        if (fmt == null)
            setFragment(false, new TranslateFragment());
    }

    private final String TAG = "Fragment";
    public void setFragment(boolean addToBackStack, Fragment fragment) {
        if (getCurrentFragmentClass() != null && getCurrentFragmentClass() == fragment.getClass())
            return;

        if (addToBackStack)
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment, TAG)
                    .addToBackStack(null)
                    .commit();
            else
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_translate)
            setFragment(false, new TranslateFragment());
        else if (id == R.id.nav_history)
            setFragment(false, new HistoryListFragment());
        else if (id == R.id.nav_starred)
            setFragment(false, new StarredListFragment());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    @Nullable
    private Class getCurrentFragmentClass() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);

        return fragment == null ? null : fragment.getClass();
    }
}
