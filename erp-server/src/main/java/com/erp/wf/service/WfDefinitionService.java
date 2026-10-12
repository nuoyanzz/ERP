package com.erp.wf.service;

import com.erp.common.api.PageResult;
import com.erp.wf.domain.dto.WfDefinitionSaveRequest;
import com.erp.wf.domain.vo.WfDefinitionVO;

import java.util.List;

public interface WfDefinitionService {

    PageResult<WfDefinitionVO> pageDefinition(int pageNum, int pageSize, String definitionKey, String definitionName, Integer status);

    WfDefinitionVO getDefinitionDetail(Long id);

    Long createDefinition(WfDefinitionSaveRequest request);

    void updateDefinition(WfDefinitionSaveRequest request);

    void deleteDefinition(Long id);

    void publishDefinition(Long id);

    void disableDefinition(Long id);

    /**
     * 按业务类型查询流程定义。
     *
     * @param bizType   业务类型
     * @param onlyEnabled 是否只返回已发布的定义
     */
    List<WfDefinitionVO> listByBizType(String bizType, boolean onlyEnabled);

    /**
     * 按业务类型查询已发布的流程定义，未配置时返回 null。
     */
    WfDefinitionVO getPublishedByBizType(String bizType);
}








