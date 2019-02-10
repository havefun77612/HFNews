package com.example.hfnews;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ListView listView;
    ArrayAdapter arrayAdapter;
    ArrayList<String> titels;
    ArrayList<String> content;
    SQLiteDatabase articlsDB;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.updatenow, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.Now:
             //   updatenow();

            default:
            return super.onOptionsItemSelected(item);
        }
        }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.NewsHeader);
        titels = new ArrayList<String>();
        content = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_expandable_list_item_1, titels);
        listView.setAdapter(arrayAdapter);

        articlsDB = this.openOrCreateDatabase("Articlestb", MODE_PRIVATE, null);
        articlsDB.execSQL("create table if not exists articl (id integer primary key ,articalid integer ,title varchar ,content varchar )");
        updatelistview();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), Articl.class);
                intent.putExtra("content", content.get(i));
                startActivity(intent);
            }
        });


        try {

          //  downloadtask task = new downloadtask();
            //task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
       new downloadtask().execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            Toast.makeText(this, "Error Calling The download task Class ", Toast.LENGTH_SHORT).show();
        }


    }


    public void updatelistview() {

        Cursor C = articlsDB.rawQuery("select * from articl", null);

        int contentindx = C.getColumnIndex("content");
        int titleindx = C.getColumnIndex("title");


        if (C.moveToFirst()) {
            titels.clear();
            content.clear();

            do {
                titels.add(C.getString(titleindx));
                content.add(C.getString(contentindx));
            } while (C.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }


    void updatenow(){
        Cursor C = articlsDB.rawQuery("select * from articl", null);

        int contentindx = C.getColumnIndex("content");
        int titleindx = C.getColumnIndex("title");


            do {
                titels.add(C.getString(titleindx));
                content.add(C.getString(contentindx));
            } while (C.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }



    public class downloadtask extends AsyncTask<String, Void, String> {

        InputStream in;
        InputStreamReader reader;

        @Override
        protected String doInBackground(String... id) {
            String result = "";
            URL url;
            HttpURLConnection httpURLConnection = null;

            try {
                url = new URL(id[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();

                InputStream in = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data != -1) {
                    char c = (char) data;
                    result += c;
                    data = reader.read();
                }

                Log.i("Content", result);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error In Url", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error Openning Connection", Toast.LENGTH_SHORT).show();
            }

            try {
                JSONArray jsonArray = new JSONArray(result);

                int numberoftopic = 25;
                //for removing the last results
                articlsDB.execSQL("delete from articl");
                if (numberoftopic > jsonArray.length()) {
                    numberoftopic = jsonArray.length();
                }
                int data = 0;
                for (int i = 0; i < numberoftopic; i++) {
                    String articlnumebr = jsonArray.getString(i);
                    String info = "";
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articlnumebr + ".json?print=pretty");
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    in = httpURLConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();

                    while (data != -1) {
                        char c = (char) data;
                        info += c;
                        data = reader.read();
                    }


                    // System.out.println(articlnumebr);
                    JSONObject jsonObject = new JSONObject(info);

                    if (!jsonObject.isNull("url") && !jsonObject.isNull("title")) {
                        String TitleofArticl = jsonObject.getString("title");
                        String urlofArticl = jsonObject.getString("url");
                        //  Log.i("Info", TitleofArticl + urlofArticl);

                        url = new URL(urlofArticl);
                        httpURLConnection = (HttpURLConnection) url.openConnection();
                        in = httpURLConnection.getInputStream();
                        reader = new InputStreamReader(in);
                        String content = "";

                        data = reader.read();

                        while (data != -1) {
                            char c = (char) data;
                            content += c;
                            data = reader.read();
                        }
                        String sql = "insert into articl (articalid,title,content) values (? , ? ,?)";
                        SQLiteStatement statement = articlsDB.compileStatement(sql);
                        statement.bindString(1, articlnumebr);
                        statement.bindString(2, TitleofArticl);
                        statement.bindString(3, content);
                        statement.execute();
                        System.out.print(content);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            updatelistview();
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updatelistview();
        }
    }

}