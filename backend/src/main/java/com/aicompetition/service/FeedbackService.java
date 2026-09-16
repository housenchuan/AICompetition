package com.aicompetition.service;

import com.aicompetition.entity.FeedbackItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class FeedbackService implements InitializingBean {

    private static final String FILE_PATH = "data/feedbacks.json";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper mapper = new ObjectMapper();
    private List<FeedbackItem> items;
    private final AtomicInteger idCounter = new AtomicInteger(4999);

    @Override
    public void afterPropertiesSet() throws Exception {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            items = Collections.synchronizedList(new ArrayList<>(seedData()));
            writeFile();
        } else {
            List<FeedbackItem> loaded = mapper.readValue(file, new TypeReference<List<FeedbackItem>>() {});
            items = Collections.synchronizedList(new ArrayList<>(loaded));
        }
        items.stream()
            .map(i -> {
                try { return Integer.parseInt(i.getId().substring(1)); }
                catch (Exception e) { return 4999; }
            })
            .max(Integer::compare)
            .ifPresent(idCounter::set);
    }

    public synchronized Map<String, Object> list(int page, int size, String type, String status,
                                                   String priority, String keyword) {
        List<FeedbackItem> filtered = items.stream()
            .filter(i -> type == null || type.isEmpty() || type.equals(i.getType()))
            .filter(i -> status == null || status.isEmpty() || status.equals(i.getStatus()))
            .filter(i -> priority == null || priority.isEmpty() || priority.equals(i.getPriority()))
            .filter(i -> keyword == null || keyword.isEmpty()
                || (i.getTitle() != null && i.getTitle().contains(keyword))
                || (i.getId() != null && i.getId().contains(keyword))
                || (i.getRelatedNo() != null && i.getRelatedNo().contains(keyword)))
            .sorted(Comparator.comparing(FeedbackItem::getCreatedAt).reversed())
            .collect(Collectors.toList());

        int total = filtered.size();
        int from = (page - 1) * size;
        List<FeedbackItem> paged = from >= total ? Collections.emptyList()
            : filtered.subList(from, Math.min(from + size, total));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("list", paged);
        return result;
    }

    public synchronized Map<String, Object> stats() {
        List<FeedbackItem> snapshot = new ArrayList<>(items);

        long total = snapshot.size();
        long pending = snapshot.stream().filter(i -> "待处理".equals(i.getStatus())).count();
        long resolvedOrClosed = snapshot.stream()
            .filter(i -> "已解决".equals(i.getStatus()) || "已关闭".equals(i.getStatus())).count();

        OptionalDouble avg = snapshot.stream()
            .filter(i -> i.getSatisfaction() != null)
            .mapToInt(FeedbackItem::getSatisfaction)
            .average();
        double avgSatisfaction = avg.isPresent() ? (Math.round(avg.getAsDouble() * 10.0)) / 10.0 : 0.0;

        String[] typeOrder = {"问题反馈", "功能建议", "误判申诉", "规则优化", "投诉", "表扬"};
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : typeOrder) {
            byType.put(t, snapshot.stream().filter(i -> t.equals(i.getType())).count());
        }

        String[] statusOrder = {"待处理", "处理中", "已解决", "已关闭", "已驳回"};
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String s : statusOrder) {
            byStatus.put(s, snapshot.stream().filter(i -> s.equals(i.getStatus())).count());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("pending", pending);
        result.put("resolvedOrClosed", resolvedOrClosed);
        result.put("avgSatisfaction", avgSatisfaction);
        result.put("byType", byType);
        result.put("byStatus", byStatus);
        return result;
    }

    public synchronized FeedbackItem create(FeedbackItem item) {
        item.setId("F" + idCounter.incrementAndGet());
        item.setStatus("待处理");
        String now = LocalDateTime.now().format(FMT);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        items.add(0, item);
        writeFile();
        return item;
    }

    public synchronized boolean update(String id, FeedbackItem patch) {
        for (FeedbackItem item : items) {
            if (item.getId().equals(id)) {
                if (patch.getStatus() != null) item.setStatus(patch.getStatus());
                if (patch.getHandler() != null) item.setHandler(patch.getHandler());
                if (patch.getSatisfaction() != null) item.setSatisfaction(patch.getSatisfaction());
                item.setUpdatedAt(LocalDateTime.now().format(FMT));
                writeFile();
                return true;
            }
        }
        return false;
    }

    private void writeFile() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), items);
        } catch (Exception e) {
            throw new RuntimeException("写入反馈文件失败: " + e.getMessage(), e);
        }
    }

    private List<FeedbackItem> seedData() {
        // [id, type, source, priority, relatedNo, title, status, handler, satisfaction, createdAt]
        Object[][] seeds = {
            {"F5000","问题反馈","核保员","中","D3000","核保结论导出 Excel 后「关键因子」列错位","待处理",null,null,"2025-06-08 10:00:00"},
            {"F5001","误判申诉","代理人","高","D3001","客户 BMI 23.5 被判为超重，请求复核","处理中","产品王",null,"2025-05-30 14:00:00"},
            {"F5002","规则优化","内部质检","中",null,"建议将甲状腺结节(良性)评分由 +10 下调","已解决","产品王",4,"2025-06-10 09:00:00"},
            {"F5003","功能建议","核保员","低",null,"希望支持按来源渠道筛选反馈","已关闭","产品王",4,"2025-05-27 11:00:00"},
            {"F5004","投诉","客户","高","D3005","等待核保超过 7 天未收到任何通知","已驳回","产品王",null,"2025-06-01 16:00:00"},
            {"F5005","表扬","代理人","低",null,"系统预测准确率提升明显，使用体验很好","已关闭","产品王",5,"2025-05-20 08:00:00"},
            {"F5006","问题反馈","系统巡检","中",null,"批量预测接口偶尔超时（>10s）","处理中","产品王",null,"2025-06-12 09:30:00"},
            {"F5007","功能建议","核保员","中",null,"核保结果页新增「发送通知」快捷按钮","待处理",null,null,"2025-06-14 10:00:00"},
            {"F5008","规则优化","内部质检","低",null,"职业风险加分中「高空作业」分类不够细化","已解决","产品王",4,"2025-06-05 14:00:00"},
            {"F5009","误判申诉","客户","高","D3010","家族史仅祖父患糖尿病被判高风险，请复核","待处理",null,null,"2025-06-15 09:00:00"},
            {"F5010","表扬","核保员","低",null,"智能助手 NLP 解析非常精准","已关闭","产品王",5,"2025-05-18 15:00:00"},
            {"F5011","问题反馈","代理人","中",null,"历史画像页面数据加载缓慢","处理中","产品王",null,"2025-06-13 11:00:00"},
            {"F5012","功能建议","内部质检","低",null,"增加批量导出核保结果为 PDF 功能","已驳回","产品王",null,"2025-06-03 09:00:00"},
            {"F5013","误判申诉","客户","高","D3015","血压 130/85 被标记为高血压项扣分，请复核","已解决","产品王",4,"2025-06-07 16:00:00"},
            {"F5014","规则优化","核保员","中",null,"驾驶员职业与「车辆行驶」活动存在重复扣分","已驳回","产品王",null,"2025-06-11 10:00:00"},
            {"F5015","投诉","客户","中","D3020","核保结论通知邮件格式混乱，无法解读","待处理",null,null,"2025-06-16 08:00:00"},
        };

        List<FeedbackItem> list = new ArrayList<>();
        for (Object[] s : seeds) {
            FeedbackItem item = new FeedbackItem();
            item.setId((String) s[0]);
            item.setType((String) s[1]);
            item.setSource((String) s[2]);
            item.setPriority((String) s[3]);
            item.setRelatedNo((String) s[4]);
            item.setTitle((String) s[5]);
            item.setStatus((String) s[6]);
            item.setHandler((String) s[7]);
            item.setSatisfaction(s[8] != null ? (Integer) s[8] : null);
            item.setCreatedAt((String) s[9]);
            item.setUpdatedAt((String) s[9]);
            list.add(item);
        }
        return list;
    }
}
