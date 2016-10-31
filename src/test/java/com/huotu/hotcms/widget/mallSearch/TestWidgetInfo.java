/*
 * 版权所有:杭州火图科技有限公司
 * 地址:浙江省杭州市滨江区西兴街道阡陌路智慧E谷B幢4楼
 *
 * (c) Copyright Hangzhou Hot Technology Co., Ltd.
 * Floor 4,Block B,Wisdom E Valley,Qianmo Road,Binjiang District
 * 2013-2016. All rights reserved.
 */

package com.huotu.hotcms.widget.mallSearch;

import com.huotu.hotcms.service.entity.Link;
import com.huotu.hotcms.service.entity.MallProductCategory;
import com.huotu.hotcms.service.model.MallProductCategoryModel;
import com.huotu.hotcms.service.repository.GalleryItemRepository;
import com.huotu.hotcms.service.repository.LinkRepository;
import com.huotu.hotcms.service.repository.MallProductCategoryRepository;
import com.huotu.hotcms.widget.CMSContext;
import com.huotu.hotcms.widget.ComponentProperties;
import com.huotu.hotcms.widget.Widget;
import com.huotu.hotcms.widget.WidgetStyle;
import com.huotu.widget.test.Editor;
import com.huotu.widget.test.WidgetTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author CJ
 */
public class TestWidgetInfo extends WidgetTest {

    @Override
    protected boolean printPageSource() {
        return false;
    }

    @Override
    protected void editorWork(Widget widget, Editor editor, Supplier<Map<String, Object>> currentWidgetProperties) {

        if (widget instanceof WidgetInfo) {
            WidgetInfo widgetInfo = (WidgetInfo) widget;
            Link link = widgetInfo.initLink(widgetInfo.initCategory(null, ""));
            editor.chooseLink(WidgetInfo.LINK_SERIAL, link);
            MallProductCategory mallProductCategory = widgetInfo.initMallProductCategory(null);
            editor.chooseCategory(WidgetInfo.MALL_PRODUCT_SERIAL, mallProductCategory);
            editor.chooseCategory(WidgetInfo.SEARCH_MALL_PRODUCT_SERIAL, mallProductCategory);
            Map map = currentWidgetProperties.get();
            assertThat(map.get(WidgetInfo.LINK_SERIAL)).isEqualTo(link.getSerial());
            assertThat(map.get(WidgetInfo.MALL_PRODUCT_SERIAL)).isEqualTo(mallProductCategory.getSerial());
            assertThat(map.get(WidgetInfo.SEARCH_MALL_PRODUCT_SERIAL)).isEqualTo(mallProductCategory.getSerial());
        }
    }

    @Override
    protected void browseWork(Widget widget, WidgetStyle style, Function<ComponentProperties, WebElement> uiChanger)
            throws IOException {
        ComponentProperties properties = widget.defaultProperties(resourceService);
        WebElement webElement = uiChanger.apply(properties);
        String serial = (String) properties.get(WidgetInfo.LINK_SERIAL);
        String mallProductSerial = (String) properties.get(WidgetInfo.MALL_PRODUCT_SERIAL);
        LinkRepository linkRepository = widget.getCMSServiceFromCMSContext(LinkRepository.class);
        Link link = linkRepository.findByCategory_SiteAndSerial(CMSContext.RequestContext().getSite(), serial);

        if (link.getLinkType().isPage()) {
            assertThat(webElement.findElement(By.className("isPage"))).as("页面包含是page的a链接").isNotNull();
        } else {
            assertThat(webElement.findElement(By.className("notPage"))).as("页面包含不是page的a链接").isNotNull();
        }

        MallProductCategoryRepository mallProductCategoryRepository = widget.getCMSServiceFromCMSContext(MallProductCategoryRepository.class);
        List<MallProductCategory> mallProductCategories = mallProductCategoryRepository
                .findBySiteAndParent_Serial(CMSContext.RequestContext().getSite(), mallProductSerial);
        GalleryItemRepository galleryItemRepository = widget.getCMSServiceFromCMSContext(GalleryItemRepository.class);
        List<MallProductCategoryModel> list = new ArrayList<>();
        for (MallProductCategory mallProductCategory : mallProductCategories) {
            MallProductCategoryModel mallProductCategoryModel = mallProductCategory.toMallProductCategoryModel();
            mallProductCategoryModel.setGalleryItems(galleryItemRepository.findByGallery(mallProductCategory.getGallery()));
            list.add(mallProductCategoryModel);
        }
        assertThat(webElement.findElement(By.className("hotwords")).findElements(By.tagName("a")).size()).isEqualTo(list.size());
    }

    @Override
    protected void editorBrowseWork(Widget widget, Function<ComponentProperties, WebElement> uiChanger
            , Supplier<Map<String, Object>> currentWidgetProperties) throws IOException {
        ComponentProperties properties = widget.defaultProperties(resourceService);
        WebElement webElement = uiChanger.apply(properties);

    }
}
