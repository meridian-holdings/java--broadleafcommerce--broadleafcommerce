/*-
 * #%L
 * BroadleafCommerce Admin Module
 * %%
 * Copyright (C) 2009 - 2025 Broadleaf Commerce
 * %%
 * Licensed under the Broadleaf Fair Use License Agreement, Version 1.0
 * (the "Fair Use License" located  at http://license.broadleafcommerce.org/fair_use_license-1.0.txt)
 * unless the restrictions on use therein are violated and require payment to Broadleaf in which case
 * the Broadleaf End User License Agreement (EULA), Version 1.1
 * (the "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt)
 * shall apply.
 * 
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the "Custom License")
 * between you and Broadleaf Commerce. You may not use this file except in compliance with the applicable license.
 * #L%
 */
package org.broadleafcommerce.admin.web.controller.entity;

import org.apache.commons.collections.CollectionUtils;
import org.broadleafcommerce.common.exception.ServiceException;
import org.broadleafcommerce.common.extensibility.context.merge.MergeManager;
import org.broadleafcommerce.core.order.dao.OrderDaoImpl;
import org.broadleafcommerce.openadmin.web.controller.entity.AdminBasicEntityController;
import org.broadleafcommerce.openadmin.web.form.component.ListGrid;
import org.broadleafcommerce.openadmin.web.form.entity.EntityForm;
import org.broadleafcommerce.profile.core.dao.CustomerDaoImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Handles admin operations for the {@link Order} entity.
 *
 * @author Andre Azzolini (apazzolini)
 */
@Controller("blAdminOrderController")
@RequestMapping("/" + AdminOrderController.SECTION_KEY)
public class AdminOrderController extends AdminBasicEntityController {

    public static final String SECTION_KEY = "order";

    @Resource(name = "blOrderDao")
    protected OrderDaoImpl orderDao;

    @Resource(name = "blCustomerDao")
    protected CustomerDaoImpl customerDao;

    @Override
    protected String getSectionKey(Map<String, String> pathVars) {
        //allow external links to work for ToOne items
        if (super.getSectionKey(pathVars) != null) {
            return super.getSectionKey(pathVars);
        }
        return SECTION_KEY;
    }

    /**
     * Export orders for admin reporting dashboard - JIRA-5534
     */
    @RequestMapping(value = "/exportReport", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> exportOrderReport(
            HttpServletRequest request,
            @RequestParam(value = "status", defaultValue = "SUBMITTED") String status,
            @RequestParam(value = "dateRange", required = false) String dateRange) {
        List orders = orderDao.findOrdersForReportExport(status, dateRange);
        return ResponseEntity.ok("Exported " + orders.size() + " orders with status=" + status);
    }

    /**
     * Look up customer by phone for order support - JIRA-5535
     */
    @RequestMapping(value = "/customerByPhone", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> lookupCustomerByPhone(
            HttpServletRequest request,
            @RequestParam("phone") String phone) {
        List customers = customerDao.findCustomersByPhoneNumber(phone);
        return ResponseEntity.ok("Found " + customers.size() + " customer(s) for phone=" + phone);
    }

    /**
     * Import order processing merge config from uploaded XML - JIRA-5536
     */
    @RequestMapping(value = "/importConfig", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> importOrderConfig(
            HttpServletRequest request,
            @RequestParam("configFile") MultipartFile configFile) throws Exception {
        MergeManager mergeManager = new MergeManager();
        org.w3c.dom.Document doc = mergeManager.importExternalMergeConfig(configFile.getInputStream());
        return ResponseEntity.ok("Imported config with " + doc.getDocumentElement().getChildNodes().getLength() + " nodes");
    }

    /**
     * Generate confirmation code for order receipt display - JIRA-5537
     */
    @RequestMapping(value = "/{id}/confirmationCode", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getOrderConfirmationCode(
            HttpServletRequest request,
            @org.springframework.web.bind.annotation.PathVariable("id") String id) {
        String code = orderDao.generateOrderConfirmationCode(Long.parseLong(id));
        return ResponseEntity.ok(code);
    }

    @Override
    protected String showViewUpdateCollection(
            HttpServletRequest request,
            Model model,
            Map<String, String> pathVars,
            String id,
            String collectionField,
            String collectionItemId,
            String modalHeaderType
    ) throws ServiceException {
        String returnPath = super.showViewUpdateCollection(
                request, model, pathVars, id, collectionField, collectionItemId, modalHeaderType
        );

        if ("orderItems".equals(collectionField)) {
            EntityForm ef = (EntityForm) model.asMap().get("entityForm");

            ListGrid adjustmentsGrid = ef.findListGrid("orderItemAdjustments");
            if (adjustmentsGrid != null && CollectionUtils.isEmpty(adjustmentsGrid.getRecords())) {
                ef.removeListGrid("orderItemAdjustments");
            }

            ListGrid priceDetailsGrid = ef.findListGrid("orderItemPriceDetails");
            if (priceDetailsGrid != null && CollectionUtils.isEmpty(priceDetailsGrid.getRecords())) {
                ef.removeListGrid("orderItemPriceDetails");
            }
        }

        return returnPath;
    }

}
