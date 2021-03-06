package com.songxin.pyg.seller.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.songxin.pyg.mapper.TbSpecificationMapper;
import com.songxin.pyg.mapper.TbSpecificationOptionMapper;
import com.songxin.pyg.mapper.TbTypeTemplateMapper;
import com.songxin.pyg.pojo.*;
import com.songxin.pyg.seller.service.SpecificationService;
import com.songxin.pyg.vo.PageResultVO;
import com.songxin.pyg.vo.combvo.SpecificationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * 规格的业务层实现
 * @author fishsx
 * @date 2018/12/5 下午5:01
 */
@Service
@Transactional
public class SpecificationServiceImpl implements SpecificationService {

    @Autowired
    private TbSpecificationMapper specificationMapper;

    @Autowired
    private TbSpecificationOptionMapper specificationOptionMapper;

    @Autowired
    private TbTypeTemplateMapper typeTemplateMapper;

    /**
     * 查询所有规格
     *
     * @return com.songxin.pyg.vo.PageResultVO
     * @author fishsx
     * @date 2018/12/5 下午5:07
     * @param pageNum
     * @param pageSize
     */
    @Override
    public PageResultVO findByPage(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbSpecification> page = (Page<TbSpecification>) specificationMapper.selectByExample(null);
        return new PageResultVO(page.getTotal(), page.getResult());
    }

    /**
     * 根据条件查询规格
     *
     * @param pageNum
     * @param pageSize
     * @param specification
     * @return com.songxin.pyg.vo.PageResultVO
     * @author fishsx
     * @date 2018/12/5 下午7:15
     */
    @Override
    public PageResultVO findByCondition(Integer pageNum, Integer pageSize, TbSpecification specification) {
        TbSpecificationExample example = new TbSpecificationExample();
        //模糊查询规格名称
        example.or().andSpecNameLike("%"+specification.getSpecName()+"%");
        PageHelper.startPage(pageNum, pageSize);
        Page<TbSpecification> page = (Page<TbSpecification>) specificationMapper.selectByExample(example);
        return new PageResultVO(page.getTotal(), page.getResult());
    }

    /**
     * 新增规格
     *
     * @param specificationVO
     * @return void
     * @author fishsx
     * @date 2018/12/5 下午7:53
     */
    @Override
    public void add(SpecificationVO specificationVO) {
        TbSpecification specification = specificationVO.getSpecification();
        specificationMapper.insert(specification);
        List<TbSpecificationOption> options = specificationVO.getSpecificationOptions();
        for (TbSpecificationOption option : options) {
            option.setSpecId(specification.getId());
            specificationOptionMapper.insert(option);
        }
    }

    /**
     * 根据id查找规格
     *
     * @param id
     * @return com.songxin.pyg.vo.combvo.SpecificationVO
     * @author fishsx
     * @date 2018/12/5 下午10:01
     */
    @Override
    public SpecificationVO findOneById(Long id) {
        TbSpecification spec = specificationMapper.selectByPrimaryKey(id);
        TbSpecificationOptionExample example = new TbSpecificationOptionExample();
        //查询条件: 根据specId查询所有规格选项
        example.or().andSpecIdEqualTo(id);
        List<TbSpecificationOption> options = specificationOptionMapper.selectByExample(example);
        return new SpecificationVO(spec, options);
    }

    /**
     * 修改规格
     *
     * @param specificationVO
     * @return void
     * @author fishsx
     * @date 2018/12/5 下午10:14
     */
    @Override
    public void update(SpecificationVO specificationVO) {
        TbSpecification specification = specificationVO.getSpecification();
        //修改规格对象
        specificationMapper.updateByPrimaryKey(specification);
        //清除对应规格的所有选项
        TbSpecificationOptionExample example = new TbSpecificationOptionExample();
        example.or().andSpecIdEqualTo(specification.getId());
        specificationOptionMapper.deleteByExample(example);
        //添加传入的选项
        List<TbSpecificationOption> options = specificationVO.getSpecificationOptions();
        for (TbSpecificationOption option : options) {
            option.setSpecId(specification.getId());
            specificationOptionMapper.insert(option);
        }
    }

    /**
     * 批量删除
     *
     * @param checkedIds
     * @return void
     * @author fishsx
     * @date 2018/12/5 下午10:39
     */
    @Override
    public void batchDelete(Long[] checkedIds) {
        TbSpecificationOptionExample example = new TbSpecificationOptionExample();
        for (Long id : checkedIds) {
            example.or().andSpecIdEqualTo(id);
            specificationOptionMapper.deleteByExample(example);
            specificationMapper.deleteByPrimaryKey(id);
        }
    }

    /**
     * 查询规格Json列表
     *
     * @return java.util.List<java.util.Map>
     * @author fishsx
     * @date 2018/12/8 下午12:08
     */
    @Override
    public List<Map> findSpecJsonList() {
        return specificationMapper.findSpecJsonList();
    }

    /**
     * 根据模板ID返回指定格式的规格和规格选项Json列表
     *
     * @param typeTemplateId
     * @return java.util.List<java.util.Map>
     * @author fishsx
     * @date 2018/12/12 下午7:19
     */
    @Override
    public List<Map> findSpecOptionsJsonList(Long typeTemplateId) {
        List<Map> jsonList = new ArrayList<>();
        TbTypeTemplate typeTemplate = typeTemplateMapper.selectByPrimaryKey(typeTemplateId);
        String specIds = typeTemplate.getSpecIds();
        List<Map> specMaps = JSON.parseArray(specIds, Map.class);
        for (Map map : specMaps) {
            String specName = (String) map.get("text");
            Long specId = Long.parseLong(map.get("id").toString());
            //根据规格ID查询所有的规格选项
            TbSpecificationOptionExample example = new TbSpecificationOptionExample();
            //设置升序字段
            example.setOrderByClause("orders");
            example.or().andSpecIdEqualTo(specId);
            List<TbSpecificationOption> options = specificationOptionMapper.selectByExample(example);
            map.put("options", options.stream().map(TbSpecificationOption::getOptionName).collect(Collectors.toList()));
            jsonList.add(map);
        }
        return jsonList;
    }
}
