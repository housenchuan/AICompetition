package com.aicompetition.common;

import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 分页结果包装。
 */
public class PageResult<T> {

    private long total;
    private int pageNum;
    private int pageSize;
    private int pages;
    private List<T> list;

    public PageResult() {}

    public PageResult(List<T> list) {
        PageInfo<T> info = new PageInfo<>(list);
        this.total = info.getTotal();
        this.pageNum = info.getPageNum();
        this.pageSize = info.getPageSize();
        this.pages = info.getPages();
        this.list = info.getList();
    }

    public static <T> PageResult<T> of(List<T> list) {
        return new PageResult<>(list);
    }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
}
