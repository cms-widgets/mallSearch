/*
 * 版权所有:杭州火图科技有限公司
 * 地址:浙江省杭州市滨江区西兴街道阡陌路智慧E谷B幢4楼
 *
 * (c) Copyright Hangzhou Hot Technology Co., Ltd.
 * Floor 4,Block B,Wisdom E Valley,Qianmo Road,Binjiang District
 * 2013-2016. All rights reserved.
 */

package com.huotu.hotcms.widget.mallSearch;

import com.huotu.hotcms.service.common.ContentType;
import com.huotu.hotcms.service.common.LinkType;
import com.huotu.hotcms.service.entity.Category;
import com.huotu.hotcms.service.entity.Link;
import com.huotu.hotcms.service.entity.MallProductCategory;
import com.huotu.hotcms.service.exception.PageNotFoundException;
import com.huotu.hotcms.service.repository.CategoryRepository;
import com.huotu.hotcms.service.repository.LinkRepository;
import com.huotu.hotcms.service.repository.MallProductCategoryRepository;
import com.huotu.hotcms.service.service.CategoryService;
import com.huotu.hotcms.service.service.ContentService;
import com.huotu.hotcms.service.service.LinkService;
import com.huotu.hotcms.widget.*;
import com.huotu.hotcms.widget.entity.PageInfo;
import com.huotu.hotcms.widget.repository.PageInfoRepository;
import com.huotu.hotcms.widget.service.PageService;
import me.jiangcai.lib.resource.service.ResourceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * @author CJ
 */
public class WidgetInfo implements Widget, PreProcessWidget {
    public static final String LINK_SERIAL = "linkSerial";
    public static final String SEARCH_MALL_PRODUCT_SERIAL = "searchMallProductSerial";
    public static final String LINK_LIST = "linkList";
    private static final Log log = LogFactory.getLog(WidgetInfo.class);


    @Override
    public String groupId() {
        return "com.huotu.hotcms.widget.mallSearch";
    }

    @Override
    public String widgetId() {
        return "mallSearch";
    }

    @Override
    public String name(Locale locale) {
        if (locale.equals(Locale.CHINA)) {
            return "搜索组件";
        }
        return "mallSearch";
    }

    @Override
    public String description(Locale locale) {
        if (locale.equals(Locale.CHINA)) {
            return "这是一个搜索组件，你可以对组件进行自定义修改。";
        }
        return "This is a mallSearch,  you can make custom change the component.";
    }

    @Override
    public String dependVersion() {
        return "1.0";
    }

    @Override
    public WidgetStyle[] styles() {
        return new WidgetStyle[]{new DefaultWidgetStyle()};
    }

    @Override
    public Resource widgetDependencyContent(MediaType mediaType) {
        if (mediaType.equals(Widget.Javascript))
            return new ClassPathResource("js/widgetInfo.js", getClass().getClassLoader());
        return null;
    }

    @Override
    public Map<String, Resource> publicResources() {
        Map<String, Resource> map = new HashMap<>();
        map.put("thumbnail/defaultStyleThumbnail.png", new ClassPathResource("thumbnail/defaultStyleThumbnail.png"
                , getClass().getClassLoader()));
        return map;
    }

    @Override
    public void valid(String styleId, ComponentProperties componentProperties) throws IllegalArgumentException {
        WidgetStyle style = WidgetStyle.styleByID(this, styleId);
        //加入控件独有的属性验证

    }

    @Override
    public Class springConfigClass() {
        return null;
    }


    @Override
    public ComponentProperties defaultProperties(ResourceService resourceService) throws IOException {
        ComponentProperties properties = new ComponentProperties();
        CategoryRepository categoryRepository = getCMSServiceFromCMSContext(CategoryRepository.class);

        MallProductCategoryRepository mallProductCategoryRepository = CMSContext.RequestContext()
                .getWebApplicationContext().getBean(MallProductCategoryRepository.class);
        //todo 过滤已经删除的控件
        List<MallProductCategory> mallProductCategoryList = mallProductCategoryRepository.findBySiteAndDeletedFalse(CMSContext
                .RequestContext().getSite());

        if (mallProductCategoryList.isEmpty()) {
            MallProductCategory mallProductCategory = initMallProductCategory(null);
            initMallProductCategory(mallProductCategory);
            properties.put(SEARCH_MALL_PRODUCT_SERIAL, mallProductCategory.getSerial());
        } else {
            properties.put(SEARCH_MALL_PRODUCT_SERIAL, mallProductCategoryList.get(0).getSerial());
        }
        //todo 过滤已经删除的控件
        List<Category> links = categoryRepository.findBySiteAndContentTypeAndDeletedFalse(CMSContext.RequestContext().getSite(), ContentType.Link);
        if (links != null && links.isEmpty()) {
            Category category = initCategory(null, "链接数据源");
            initLink(category);
            properties.put(LINK_SERIAL, category.getSerial());
        } else {
            properties.put(LINK_SERIAL, links.get(0).getSerial());
        }
        return properties;
    }

    @Override
    public void prepareContext(WidgetStyle style, ComponentProperties properties, Map<String, Object> variables
            , Map<String, String> parameters) {
        String linkSerial = (String) variables.get(LINK_SERIAL);
        LinkRepository linkRepository = getCMSServiceFromCMSContext(LinkRepository.class);
        List<Link> links = linkRepository.findByCategory_SiteAndCategory_Serial(CMSContext.RequestContext().getSite(), linkSerial);
        PageInfoRepository pageInfoRepository = getCMSServiceFromCMSContext(PageInfoRepository.class);
        for (Link link : links) {
            if (link.getLinkType().isPage()) {
                PageInfo pageInfo = pageInfoRepository.getOne(link.getPageInfoID());
                link.setPagePath(pageInfo != null ? pageInfo.getPagePath() : "");
            }
        }
        variables.put(LINK_LIST, links);
    }


    /**
     * 初始化数据源
     *
     * @param parent
     * @param name
     * @return
     */
    public Category initCategory(Category parent, String name) {
        CategoryService categoryService = getCMSServiceFromCMSContext(CategoryService.class);
        CategoryRepository categoryRepository = getCMSServiceFromCMSContext(CategoryRepository.class);
        Category category = new Category();
        category.setContentType(ContentType.Link);
        category.setName(name);
        categoryService.init(category);
        category.setSite(CMSContext.RequestContext().getSite());
        category.setParent(parent);
        //保存到数据库
        categoryRepository.save(category);
        return category;
    }

    /**
     * 初始化一个图片
     *
     * @param category
     */
    public Link initLink(Category category) {
        ContentService contentService = getCMSServiceFromCMSContext(ContentService.class);
        LinkService linkService = getCMSServiceFromCMSContext(LinkService.class);
        Link link = new Link();
        link.setTitle("link");
        link.setCategory(category);
        link.setDeleted(false);
        link.setCreateTime(LocalDateTime.now());
        link.setLinkUrl("http://www.huobanplus.com/");
        contentService.init(link);
        link.setLinkType(LinkType.Link);
        linkService.saveLink(link);
        return link;
    }

    public MallProductCategory initMallProductCategory(MallProductCategory parent) {
        CategoryService categoryService = getCMSServiceFromCMSContext(CategoryService.class);
        MallProductCategoryRepository mallProductCategoryRepository = getCMSServiceFromCMSContext(MallProductCategoryRepository.class);
        MallProductCategory mallProductCategory = new MallProductCategory();
        mallProductCategory.setGoodTitle("");
        mallProductCategory.setSite(CMSContext.RequestContext().getSite());
        mallProductCategory.setName("商城产品数据源");
        mallProductCategory.setCreateTime(LocalDateTime.now());
        mallProductCategory.setContentType(ContentType.MallProduct);
        mallProductCategory.setParent(parent);
        categoryService.init(mallProductCategory);
        mallProductCategoryRepository.save(mallProductCategory);
        return mallProductCategory;
    }

    public void setContentURI(Map<String, Object> variables, MallProductCategory mallProductCategory) {
        try {
            PageInfo contentPage = getCMSServiceFromCMSContext(PageService.class)
                    .getClosestContentPage(mallProductCategory, (String) variables.get("uri"));
            mallProductCategory.setContentURI(contentPage.getPagePath());
        } catch (PageNotFoundException e) {
            log.warn("...", e);
            mallProductCategory.setContentURI((String) variables.get("uri"));
        }
    }

}
