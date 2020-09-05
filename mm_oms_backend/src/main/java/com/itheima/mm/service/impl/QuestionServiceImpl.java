package com.itheima.mm.service.impl;

import com.itheima.framework.annotation.HmComponent;
import com.itheima.mm.base.BaseService;
import com.itheima.mm.common.QuestionConst;
import com.itheima.mm.dao.*;
import com.itheima.mm.database.MmDaoException;
import com.itheima.mm.entity.PageResult;
import com.itheima.mm.entity.QueryPageBean;
import com.itheima.mm.pojo.*;
import com.itheima.mm.service.QuestionService;
import com.itheima.mm.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;

import java.util.*;

/**
 * @author ：seanyang
 * @date ：Created in 2019/8/12
 * @description ：题目业务实现类
 * @version: 1.0
 */
@HmComponent("questionService")
@Slf4j
public class QuestionServiceImpl extends BaseService implements QuestionService {
	@Override
	public PageResult findByPage(QueryPageBean queryPageBean) {
		log.info("QuestionService findByPage:{}",queryPageBean);
		SqlSession sqlSession = getSession();
		QuestionDao questionDao = getDao(sqlSession,QuestionDao.class);
		if(queryPageBean.getQueryParams() == null){
			queryPageBean.setQueryParams(new HashMap());
		}
		queryPageBean.getQueryParams().put("isClassic",0);
		// 获取数据集
		List<Question> questionList = questionDao.selectIsClassicByPage(queryPageBean);
		// 获取数据记录格式
		Long totalCount = questionDao.selectCountIsClassicByPage(queryPageBean);
		closeSession(sqlSession);
		return new PageResult(totalCount,questionList);
	}

	@Override
	public void addOrUpdate(Question question) {
		SqlSession sqlSession = getSession();
		try{
			log.info("QuestionService question:{}",question);
			// 1 添加题目信息
			if( addOrUpdateQuestion(sqlSession,question) == 0){
				throw new MmDaoException("更新数据失败");
			}
			log.debug("QuestionService question.id:{}",question.getId());

			// 2.更新题目选项信息
			updateQuestionItemList(sqlSession,question);
			// 3.更新标签信息
			updateTagList(sqlSession,question);
			// 4.更新公司信息(行业信息)
			updateCompany(sqlSession,question);
			// 提交保存
			commitAndCloseSession(sqlSession);
			log.debug("dao question:{}",question);

		}catch(MmDaoException e){
			log.error("addOrUpdate",e);
			rollbackAndCloseSession(sqlSession);
			throw  new MmDaoException(e.getMessage());
		}
	}

	// 新增或更新题目
	private Integer addOrUpdateQuestion(SqlSession sqlSession,Question question){
		log.debug("QuestionService addOrUpdateQuestion:{}",question);
		QuestionDao questionDao = getDao(sqlSession,QuestionDao.class);
		Integer result = 0;
		question.setCompanyId(question.getCompany().getId());
		if(question.getId()==null  || question.getId()==0 ){
			// 初始化题目相关信息
			question.setCreateDate(DateUtils.parseDate2String(new Date()));
			// 通过枚举下标，获取的值对应数据需要的值
			// 初始化题目状态及审核状态
			question.setStatus(QuestionConst.Status.PRE_PUBLISH.ordinal());
			question.setReviewStatus(QuestionConst.ReviewStatus.PRE_REVIEW.ordinal());
			result = questionDao.add(question);
		}else{
			result = questionDao.update(question);
		}
		return result;
	}

	// 更新题目选项信息
	private void updateQuestionItemList(SqlSession sqlSession,Question question){
		log.info("QuestionService updateQuestionItemList:{}",question.getQuestionItemList());
		QuestionItemDao questionItemDao = getDao(sqlSession,QuestionItemDao.class);
		for (QuestionItem questionItem:question.getQuestionItemList()){
			// 设置应对的题目ID
			questionItem.setQuestionId(question.getId());
			if(questionItem.getContent() == null || questionItem.getContent().length() ==0){
				if(questionItem.getImgUrl() == null || questionItem.getImgUrl().length() == 0)
					continue;
			}
			if(questionItem.getId() == null || questionItem.getId() ==0){
				// 新增
				questionItemDao.addQuestionItem(questionItem);
			}else{
				// 更新
				questionItemDao.updateQuestionItem(questionItem);
			}
		}
	}

	// 更新标签信息
	private void updateTagList(SqlSession sqlSession ,Question question){
		log.info("QuestionService updateTagList:{}",question.getTagList());
		TagDao tagDao = getDao(sqlSession,TagDao.class);
		// 删除之前的题目标签关系
		tagDao.deleteTagByQuestionId(question.getId());
		for (Tag tag:question.getTagList()){
			Map<String,Object> mapQuestionTag = new HashMap<>();
			mapQuestionTag.put("questionId",question.getId());
			Integer tagId = 0;
			// 新增标签
			if(tag.getId() == 0){
				tag.setStatus(QuestionConst.TagStatus.ENABLE.ordinal());
				tag.setUserId(question.getUserId());
				tag.setCreateDate(DateUtils.parseDate2String(new Date()));
				tag.setCourseId(question.getCourseId());
				tagDao.addTag(tag);
			}
			mapQuestionTag.put("tagId",tag.getId());
			tagDao.addTagByQuestionId(mapQuestionTag);
		}
	}
	// 更新公司信息
	private void updateCompany(SqlSession sqlSession,Question question){
		log.info("QuestionService updateCompany:{}",question.getCompany());
		IndustryDao industryDao = getDao(sqlSession,IndustryDao.class);
		CompanyDao companyDao = getDao(sqlSession,CompanyDao.class);
		Company company = question.getCompany();
		// 更新城市关系
		company.setUserId(question.getUserId());
		companyDao.updateCompanyCity(company);
		// 删除旧企业方向关系
		companyDao.deleteCompanyIndustryByCompanyId(company.getId());
		// 新增新的行业方向及关系
		List<Industry> industryList = company.getIndustryList();
		for (Industry industry:industryList){
			Map<String,Object> one = new HashMap<>();
			one.put("companyId",company.getId());
			if(industry.getId() == 0){
				// 新增行业方向
				industryDao.add(industry);
			}
			one.put("industryId",industry.getId());
			companyDao.addCompanyIndustry(one);
		}
	}

	@Override
	public PageResult findClassicByPage(QueryPageBean queryPageBean) {
		log.info("QuestionService findClassicByPage:{}",queryPageBean);
		SqlSession sqlSession = getSession();
		QuestionDao questionDao = getDao(sqlSession,QuestionDao.class);
		if(queryPageBean.getQueryParams() == null){
			queryPageBean.setQueryParams(new HashMap());
		}
		// 查询精选题目
		queryPageBean.getQueryParams().put("isClassic",1);
		List<Question> questionList = questionDao.selectIsClassicByPage(queryPageBean);
		// 获取每条记录的审核状态
		for (Question question:questionList){
			ReviewLogDao reiviewLogDao = getDao(sqlSession,ReviewLogDao.class);
			ReviewLog reviewLog = reiviewLogDao.selectLastByQuestionId(question.getId());
			if (reviewLog == null){
				reviewLog = new ReviewLog();
				reviewLog.setStatus(0);
				reviewLog.setComments("");
				reviewLog.setReviewer("");
				reviewLog.setCreateDate("");
			}
			question.setReviewLog(reviewLog);
		}
		// 获取总记录数
		Long totalCount = questionDao.selectCountIsClassicByPage(queryPageBean);
		closeSession(sqlSession);
		return new PageResult(totalCount,questionList);
	}

	@Override
	public void updateIsClassic(Integer questionId, Integer isClassic) {
		SqlSession sqlSession = getSession();
		try{
			QuestionDao questionDao = getDao(sqlSession,QuestionDao.class);
			Integer result = questionDao.updateIsClassic(questionId,isClassic);
			if (result == 0){
				throw new MmDaoException("更新经典题目失败");
			}
			commitAndCloseSession(sqlSession);
		}catch(MmDaoException e){
			log.error("updateIsClassic",e);
			closeSession(sqlSession);
			throw  new MmDaoException(e.getMessage());
		}
	}

	@Override
	public Question findQuestionById(Integer id) {
		log.info("QuestionService findQuestionById:{}",id);
		SqlSession sqlSession = getSession();
		// 获取题目基本信息
		// 级联获取题目选项信息
		QuestionDao questionDao = getDao(sqlSession,QuestionDao.class);
		Question question = questionDao.selectById(id);
		log.debug("question:{}",question);
		// 获取所属标签名称列表
		initQuestionTag(sqlSession,question);
		// 根据公司ID，获取所属公司信息
		initQuestionCompany(sqlSession,question);
		closeSession(sqlSession);
		return question;
	}

	// 获取题目标签信息
	private void initQuestionTag(SqlSession sqlSession,Question question){
		log.info("QuestionService initQuestionTag:{}",question.getId());
		TagDao tagDao = getDao(sqlSession,TagDao.class);
		List<Tag> tagList = tagDao.selectTagListByQuestionId(question.getId());
		List<String> tagNameList = new ArrayList<>();
		for (Tag one:tagList){
			tagNameList.add(one.getName());
		}
		// 设置标签名称列表
		question.setTagNameList(tagNameList);
		question.setTagList(tagList);
	}

	// 获取题目公司及行业信息
	private void initQuestionCompany(SqlSession sqlSession,Question question){
		log.info("QuestionService initQuestionCompany:{}",question.getCompanyId());
		CompanyDao companyDao = getDao(sqlSession,CompanyDao.class);
		Company company = companyDao.selectByIdForQuestion(question.getCompanyId());
		// 根据公司ID，获取所属行业信息
		IndustryDao industryDao = getDao(sqlSession,IndustryDao.class);
		List<Industry> industryList = industryDao.selectIndustryListByCompany(question.getCompanyId());
		List<String> industryNameList = new ArrayList<>();
		for (Industry one:industryList){
			industryNameList.add(one.getName());
		}
		company.setIndustryNameList(industryNameList);
		company.setIndustryList(industryList);
		// 设置所属公司(包含所属城市行业)
		question.setCompany(company);
	}
}
