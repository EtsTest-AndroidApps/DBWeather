package com.dbeginc.dbweather.ui.newsdetails;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;

import com.dbeginc.dbweather.DBWeatherApplication;
import com.dbeginc.dbweather.R;
import com.dbeginc.dbweather.databinding.NewsActivityBinding;
import com.dbeginc.dbweather.models.datatypes.news.Article;
import com.dbeginc.dbweather.models.provider.image.IImageProvider;
import com.dbeginc.dbweather.ui.BaseActivity;

import javax.inject.Inject;

import static com.dbeginc.dbweather.utils.holder.ConstantHolder.CUSTOM_TAB_PACKAGE_NOT_FOUND;
import static com.dbeginc.dbweather.utils.holder.ConstantHolder.NEWS_DATA_KEY;

/**
 * Created by Darel Bitsy on 11/03/17.
 * Dialog fragment that show description
 * about the news the user clicked
 */

public class NewsDialogActivity extends BaseActivity implements INewsDialogView {
    private NewsActivityBinding mNewsDialogBinder;
    private NewsDialogPresenter mPresenter;
    @Inject IImageProvider mImageDownloader;
    private CustomTabsIntent customTabsIntent;

    @Override
    public void showImage(@NonNull final String url) {
        mPresenter.getImage(this, mNewsDialogBinder.articleImage, R.drawable.no_image_icon, mNewsDialogBinder.newsProgressBar, url);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DBWeatherApplication.getComponent()
                .inject(this);

        mNewsDialogBinder = DataBindingUtil.setContentView(this, R.layout.news_activity);
        mPresenter = new NewsDialogPresenter(mImageDownloader);

        final Article article = getIntent().getParcelableExtra(NEWS_DATA_KEY);
        mNewsDialogBinder.setArticle(article);
        setSupportActionBar(mNewsDialogBinder.newsToolbar.toolbarId);

        mNewsDialogBinder.closeButton.setOnClickListener(view -> closeView());

        if(!article.getUrlToImage().isEmpty()) {
            showImage(article.getUrlToImage());
        }

        mNewsDialogBinder.openInBrowserButton.setOnClickListener(view ->
                openArticleInBrowser(article.getArticleUrl()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        final String customTabPackage = mAppDataProvider.getCustomTabPackage();

        if (!customTabPackage.equalsIgnoreCase(CUSTOM_TAB_PACKAGE_NOT_FOUND)) {
            customTabsIntent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setToolbarColor(Color.BLUE)
                    .enableUrlBarHiding()
                    .build();
            customTabsIntent.intent.setPackage(customTabPackage);
        }
    }

    @Override
    public void openArticleInBrowser(@NonNull final String articleUrl) {
        if (customTabsIntent != null) { customTabsIntent.launchUrl(this, Uri.parse(articleUrl)); }
        else {
            final Intent browserIntent = new Intent()
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.parse(articleUrl.isEmpty() ? "https://github.com/404" : articleUrl))
                    .addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(browserIntent);
        }
    }

    @Override
    public void closeView() {
        finish();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        final Article article = getIntent().getParcelableExtra(NEWS_DATA_KEY);
        mNewsDialogBinder.setArticle(article);

        if(!article.getUrlToImage().isEmpty()) {
            showImage(article.getUrlToImage());
        }

        mNewsDialogBinder.openInBrowserButton.setOnClickListener(view ->
                openArticleInBrowser(article.getArticleUrl()));
    }
}