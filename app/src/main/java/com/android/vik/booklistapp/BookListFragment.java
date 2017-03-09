package com.android.vik.booklistapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BookListFragment extends Fragment {

    private ArrayAdapter<String> mBookAdapter;

    private int maxResults = 10;

    private String mTopic = "";

    ConnectivityManager cm;

    public BookListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        View rootView = inflater.inflate(R.layout.fragment_book_list, container, false);

        final EditText editText = (EditText) rootView.findViewById(R.id.edit_text);

        ListView listView = (ListView) rootView.findViewById(R.id.book_list_view);

        mBookAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);

        listView.setAdapter(mBookAdapter);

        rootView.findViewById(R.id.search_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

                if (editText.getText().toString().equals("")) {
                    Toast.makeText(getContext(), "Please enter some text!", Toast.LENGTH_SHORT).show();
                } else {
                    mTopic = editText.getText().toString();
                    if (activeNetwork != null &&
                            activeNetwork.isConnectedOrConnecting()) {
                        BookListFetchTask bookListFetchTask = new BookListFetchTask();
                        bookListFetchTask.execute(mTopic);
                    } else {
                        Toast.makeText(getContext(), "No internet connection!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        return rootView;
    }

    public class BookListFetchTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = BookListFetchTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String bookListJsonStr = null;

            try {

                final String BOOK_LIST_BASE_URL = " https://www.googleapis.com/books/v1/volumes?";
                final String QUERY_PARAM = "q";
                final String MAX_RESULTS_PARAM = "maxResults";

                Uri builtUri = Uri.parse(BOOK_LIST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, mTopic)
                        .appendQueryParameter(MAX_RESULTS_PARAM, Integer.toString(maxResults))
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {

                    bookListJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {

                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {

                    bookListJsonStr = null;
                }
                bookListJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e("BookList Fragment", "Error ", e);
                bookListJsonStr = null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("BookListFragment", "Error closing stream", e);
                    }
                }
            }

            try {
                return getBookListFromJson(bookListJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mBookAdapter.clear();
                for (String book : result) {
                    mBookAdapter.add(book);
                }
            } else {
                Toast.makeText(getContext(), "Sorry! No results found.", Toast.LENGTH_SHORT).show();
            }
        }

        private String[] getBookListFromJson(String bookListJsonStr) throws JSONException {
            final String OWM_LIST = "items";

            JSONObject bookListJson = new JSONObject(bookListJsonStr);
            JSONArray items = bookListJson.getJSONArray(OWM_LIST);

            String[] resultStrings = new String[maxResults];

            for (int i = 0; i < items.length(); i++) {
                JSONObject oneBook = items.getJSONObject(i);
                JSONObject volumeObject = oneBook.getJSONObject("volumeInfo");
                String title = volumeObject.getString("title");
                String author = volumeObject.getJSONArray("authors").getString(0);

                resultStrings[i] = title + " \n" + author;
            }

            return resultStrings;
        }
    }

}
