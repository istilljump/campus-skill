package com.campus.runner.service;

import com.campus.runner.dto.PortfolioAuditProcessDTO;
import com.campus.runner.dto.PortfolioDTO;
import com.campus.runner.entity.Portfolio;
import com.campus.runner.result.PageResult;
import com.campus.runner.vo.SkillerProfileVO;

public interface PortfolioService {

    /**
     * 技能者端-我的作品列表
     */
    java.util.List<Portfolio> listOwn(Long skillerId);

    /**
     * 技能者端-上传作品（待审核）
     */
    void create(Long skillerId, PortfolioDTO portfolioDTO);

    /**
     * 技能者端-删除自己的作品
     */
    void delete(Long skillerId, Long id);

    /**
     * 管理端-作品审核分页（可按状态过滤）
     */
    PageResult<Portfolio> page(Integer page, Integer pageSize, Integer status);

    /**
     * 管理端-作品审核处理（通过可评定作品等级并联动技能者 skill_level）
     */
    void process(PortfolioAuditProcessDTO portfolioAuditProcessDTO, Long adminId);

    /**
     * 用户端-技能者主页（公开信息 + 已通过作品 + 上架服务）
     */
    SkillerProfileVO getSkillerProfile(Long skillerId);
}
