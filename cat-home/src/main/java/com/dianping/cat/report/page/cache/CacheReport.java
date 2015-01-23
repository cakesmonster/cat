package com.dianping.cat.report.page.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unidal.lookup.util.StringUtils;

import com.dianping.cat.consumer.event.model.entity.EventName;
import com.dianping.cat.consumer.event.model.entity.EventType;
import com.dianping.cat.consumer.transaction.model.entity.TransactionName;
import com.dianping.cat.consumer.transaction.model.entity.TransactionType;
import com.dianping.cat.report.page.JsonBuilder;

public class CacheReport {

	private static final String ALL = "ALL";

	private String m_domain;

	private Set<String> m_domainNames = new LinkedHashSet<String>();

	private Set<String> m_domains;

	private java.util.Date m_endTime;

	private Set<String> m_ips = new LinkedHashSet<String>();

	private Map<String, CacheNameItem> m_nameItems = new HashMap<String, CacheNameItem>();

	private String m_sortBy;

	private java.util.Date m_startTime;

	private Map<String, CacheTypeItem> m_typeItems = new HashMap<String, CacheTypeItem>();

	private Set<String> m_methods = new LinkedHashSet<String>();

	public void addNewNameItem(TransactionName transactionName, EventName eventName) {
		String arrays[] = transactionName.getId().split(":");
		String categroy = arrays[0];
		String method = "";

		if (arrays.length > 1) {
			method = arrays[1];
		}
		CacheNameItem item = m_nameItems.get(categroy);
		CacheNameItem all = m_nameItems.get(ALL);

		if (all == null) {
			all = new CacheNameItem();
			all.setName(new TransactionName(ALL));
			m_nameItems.put(ALL, all);
		}
		all.add(transactionName, eventName, method);

		if (item == null) {
			item = new CacheNameItem();
			item.setName(new TransactionName(categroy));
			m_nameItems.put(categroy, item);
		}
		item.add(transactionName, eventName, method);
		m_methods.add(method);
	}

	public void addNewTypeItem(TransactionType transactionType, EventType eventType) {
		String key = transactionType.getId();
		CacheTypeItem item = m_typeItems.get(key);

		if (item == null) {
			item = new CacheTypeItem();
			item.setType(transactionType);
			item.setMissed(eventType.getTotalCount());
			item.setHited(1 - (double) eventType.getTotalCount() / transactionType.getTotalCount());
			m_typeItems.put(key, item);
		}
	}

	public String getDomain() {
		return m_domain;
	}

	public Set<String> getDomainNames() {
		return m_domainNames;
	}

	public Set<String> getDomains() {
		return m_domains;
	}

	public java.util.Date getEndTime() {
		return m_endTime;
	}

	public Set<String> getIps() {
		return m_ips;
	}

	public List<CacheNameItem> getNameItems() {
		List<CacheNameItem> result = new ArrayList<CacheNameItem>(m_nameItems.values());
		Collections.sort(result, new CacheNameItemCompator(m_sortBy));
		return result;
	}

	public java.util.Date getStartTime() {
		return m_startTime;
	}

	public List<CacheTypeItem> getTypeItems() {
		List<CacheTypeItem> result = new ArrayList<CacheTypeItem>(m_typeItems.values());
		Collections.sort(result, new CacheTypeItemCompator(m_sortBy));
		return result;
	}

	public Set<String> getMethods() {
		return m_methods;
	}

	public void setDomain(String domain) {
		m_domain = domain;
	}

	public void setDomainNames(Set<String> domainNames) {
		m_domainNames = domainNames;
	}

	public void setDomains(Set<String> domains) {
		m_domains = domains;
	}

	public void setEndTime(java.util.Date endTime) {
		m_endTime = endTime;
	}

	public void setIps(Set<String> ips) {
		m_ips = ips;
	}

	public void setSortBy(String sortBy) {
		m_sortBy = sortBy;
	}

	public void setStartTime(java.util.Date startTime) {
		m_startTime = startTime;
	}

	public String toString() {
		return new JsonBuilder().toJson(this);
	}

	public static class CacheNameItem {
		private double m_hited = 1;

		private long m_missed;

		private Map<String, Long> m_methodCounts = new HashMap<String, Long>();

		private TransactionName m_name;

		private String m_category;

		public void add(TransactionName transactionName, EventName eventName, String method) {
			long transactionTotalCount = transactionName.getTotalCount();

			m_name.setTotalCount(m_name.getTotalCount() + transactionTotalCount);
			m_name.setSum(m_name.getSum() + transactionName.getSum());
			m_name.setAvg((double) m_name.getSum() / m_name.getTotalCount());
			m_name.setTps(m_name.getTps() + transactionName.getTps());

			if (!StringUtils.isEmpty(method)) {
				long total = incMethodCount(method, transactionTotalCount);

				if ("get".equals(method)) {
					m_missed = m_missed + eventName.getTotalCount();
					m_hited = 1 - (double) m_missed / total;
				}
			}
		}

		public String getCategory() {
			return m_category;
		}

		private Long getMethodCount(String field) {
			Long value = m_methodCounts.get(field);

			if (value == null) {
				value = new Long(0);

				m_methodCounts.put(field, value);
			}
			return value;
		}

		private long incMethodCount(String method, Long value) {
			Long source = getMethodCount(method);
			long result = source + value;

			m_methodCounts.put(method, result);
			return result;
		}

		public double getHited() {
			return m_hited;
		}

		public long getMissed() {
			return m_missed;
		}

		public Map<String, Long> getMethodCounts() {
			return m_methodCounts;
		}

		public TransactionName getName() {
			return m_name;
		}

		public void setHited(double hited) {
			m_hited = hited;
		}

		public void setMissed(long missed) {
			m_missed = missed;
		}

		public void setName(TransactionName name) {
			m_name = name;
		}
	}

	public static class CacheNameItemCompator implements Comparator<CacheNameItem> {

		private String m_sort;

		private CacheNameItemCompator(String sort) {
			m_sort = sort;
		}

		@Override
		public int compare(CacheNameItem o1, CacheNameItem o2) {
			if (o1.getName().getId() != null && o1.getName().getId().startsWith(ALL)) {
				return -1;
			}
			if (o2.getName().getId() != null && o2.getName().getId().startsWith(ALL)) {
				return 1;
			}
			if (m_sort.equals("total")) {
				long result = o2.getName().getTotalCount() - o1.getName().getTotalCount();

				return result == 0 ? 0 : (result > 0 ? 1 : -1);
			} else if (m_sort.equals("missed")) {
				long result = o2.getMissed() - o1.getMissed();

				return result == 0 ? 0 : (result > 0 ? 1 : -1);
			} else if (m_sort.equals("hitPercent")) {
				double result = o1.getHited() * 1000 - o2.getHited() * 1000;

				return result == 0 ? 0 : (result > 0 ? 1 : -1);
			} else if (m_sort.equals("avg")) {
				double result = o2.getName().getAvg() * 1000 - o1.getName().getAvg() * 1000;

				return result == 0 ? 0 : (result > 0 ? 1 : -1);
			} else if (m_sort.equals("name")) {
				return o1.getName().getId().compareTo(o2.getName().getId());
			} else {
				long result = o2.getMethodCount(m_sort) - o1.getMethodCount(m_sort);

				return result == 0 ? 0 : (result > 0 ? 1 : -1);
			}
		}
	}

	public static class CacheTypeItem {
		private double m_hited;

		private long m_missed;

		private TransactionType m_type;

		public double getHited() {
			return m_hited;
		}

		public long getMissed() {
			return m_missed;
		}

		public TransactionType getType() {
			return m_type;
		}

		public void setHited(double hited) {
			m_hited = hited;
		}

		public void setMissed(long missed) {
			m_missed = missed;
		}

		public void setType(TransactionType type) {
			m_type = type;
		}
	}

	public static class CacheTypeItemCompator implements Comparator<CacheTypeItem> {

		private String m_sort;

		private CacheTypeItemCompator(String sort) {
			m_sort = sort;
		}

		@Override
		public int compare(CacheTypeItem o1, CacheTypeItem o2) {
			if (m_sort.equals("total")) {
				return (int) (o2.getType().getTotalCount() - o1.getType().getTotalCount());
			} else if (m_sort.equals("missed")) {
				return (int) (o2.getMissed() - o1.getMissed());
			} else if (m_sort.equals("hitPercent")) {
				return (int) (o1.getHited() * 1000 - o2.getHited() * 1000);
			} else if (m_sort.equals("avg")) {
				return (int) (o2.getType().getAvg() * 1000 - o1.getType().getAvg() * 1000);
			} else if (m_sort.equals("type")) {
				return o1.getType().getId().compareTo(o2.getType().getId());
			}
			return 0;
		}
	}
}
