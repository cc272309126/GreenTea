package org.ofbiz.search.solr;

import java.util.Iterator;
import java.util.List;

import javolution.util.FastList;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;

/**
 * Product category util class for solr.
 */
public abstract class CategoryUtil {
    
    public static final String module = CategoryUtil.class.getName();

    /**
     * Gets catalog IDs for specified product category.
     * <p>
     * This method is a supplement to CatalogWorker methods. 
     */
    public static List<String> getCatalogIdsByCategoryId(Delegator delegator, String productCategoryId) {
        List<String> catalogIds = FastList.newInstance();
        List<GenericValue> catalogs = null;
        try {
            EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toMap("productCategoryId", productCategoryId));
            // catalogs = delegator.findList("ProdCatalog", null, null, UtilMisc.toList("catalogName"), null, false);
            catalogs = delegator.findList("ProdCatalogCategory", condition, null, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up all catalogs", module);
        }
        if (catalogs != null) {
            for (GenericValue c : catalogs) {
                catalogIds.add(c.getString("prodCatalogId"));
            }
        }
        return catalogIds;
    }    
    
    public static List<List<String>> getCategoryTrail(String productCategoryId, DispatchContext dctx) {
       GenericDelegator delegator = (GenericDelegator) dctx.getDelegator();
        List<List<String>> trailElements = FastList.newInstance();
        String parentProductCategoryId = productCategoryId;
        while (UtilValidate.isNotEmpty(parentProductCategoryId)) {
            // find product category rollup
            try {
                List<EntityCondition> rolllupConds = FastList.newInstance();
                rolllupConds.add(EntityCondition.makeCondition("productCategoryId", parentProductCategoryId));
                rolllupConds.add(EntityUtil.getFilterByDateExpr());
                List<GenericValue> productCategoryRollups = delegator.findList("ProductCategoryRollup", EntityCondition.makeCondition(rolllupConds), null, UtilMisc.toList("-fromDate"), null, true);
                if (UtilValidate.isNotEmpty(productCategoryRollups)) {
                    List<List<String>> trailElementsAux = FastList.newInstance();
                    trailElementsAux.addAll(trailElements);
                    // add only categories that belong to the top category to trail
                    for (GenericValue productCategoryRollup : productCategoryRollups) {
                        String trailCategoryId = productCategoryRollup.getString("parentProductCategoryId");
                        parentProductCategoryId = trailCategoryId;
                        List<String> trailElement = FastList.newInstance();
                        if (!trailElements.isEmpty()) {
                            for (List<String> trailList : trailElementsAux) {
                                trailElement.add(trailCategoryId);
                                trailElement.addAll(trailList);
                                trailElements.remove(trailList);
                                trailElements.add(trailElement);
                            }
                        } else {
                            trailElement.add(trailCategoryId);
                            trailElement.add(productCategoryId);
                            trailElements.add(trailElement);
                        }
                    }
                } else {
                    parentProductCategoryId = null;
                }

            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot generate trail from product category", module);
            }
        }
        if (trailElements.size() == 0) {
            List<String> trailElement = FastList.newInstance();
        	trailElement.add(productCategoryId);
            trailElements.add(trailElement);
        }
        return trailElements;
    }
    
    /**
     * Returns categoryName with trail
     */
    public static String getCategoryNameWithTrail(String productCategoryId, DispatchContext dctx) {
    	return getCategoryNameWithTrail(productCategoryId, true,  dctx);
    }

    public static String getCategoryNameWithTrail(String productCategoryId, Boolean showDepth, DispatchContext dctx) {
    	List<List<String>> trailElements = CategoryUtil.getCategoryTrail(productCategoryId, dctx);
        //Debug.log("trailElements ======> " + trailElements.toString());
        StringBuilder catMember = new StringBuilder();
        String cm ="";
        int i = 0;
        for (List<String> trailElement : trailElements) {
            for (Iterator<String> trailIter = trailElement.iterator(); trailIter.hasNext();) {
                String trailString = (String) trailIter.next();
                if (catMember.length() > 0){
                	catMember.append("/");
                	i++;
                }
                
                catMember.append(trailString);
            }
        }
        
        if (catMember.length() == 0){catMember.append(productCategoryId);}
        
        if(showDepth) {
        	cm = i +"/"+ catMember.toString();
        } else {
        	cm = catMember.toString();
        }
        //Debug.logInfo("catMember "+cm,module);
    	return cm;
    }
    
    /**	
     * Returns nextLevel from trailed category.
     * <p>
     * Ie for "1/SYRACUS2_CATEGORY/FICTION_C/" the returned value would be 2.
     */
    public static int getNextLevelFromCategoryId(String productCategoryId, DispatchContext dctx) {
    	try{
	    	if (productCategoryId.contains("/")) {
	    		String[] productCategories = productCategoryId.split("/");
	    		int level = Integer.parseInt(productCategories[0]);
	    		return level++;
	    	} else {
	    		return 0;
	    	}
    	} catch(Exception e) {
    		return 0;
    	}
    }
    
    /**	
     * Returns proper FacetFilter from trailed category.
     * <p>
     * Ie for "1/SYRACUS2_CATEGORY/FICTION_C/" the returned value would be
     * "2/SYRACUS2_CATEGORY/FICTION_C/".
     */
    public static String getFacetFilterForCategory(String productCategoryId, DispatchContext dctx) {
    	try{
			String[] productCategories = productCategoryId.split("/");
			int level = Integer.parseInt(productCategories[0]);
			int nextLevel = level+1;
			productCategories[0] = ""+nextLevel;
			return StringUtils.join(productCategories,"/");
    	} catch(Exception e) {
    		return productCategoryId;
    	}
    }
    
}