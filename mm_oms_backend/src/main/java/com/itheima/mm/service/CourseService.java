package com.itheima.mm.service;

import com.itheima.mm.entity.PageResult;
import com.itheima.mm.entity.QueryPageBean;
import com.itheima.mm.pojo.Course;

import java.util.List;

/**
 * @author ：seanyang
 * @date ：Created in 2019/9/30
 * @description ：
 * @version: 1.0
 */
public interface CourseService {
	/**
	 * 添加学科
	 * @param course
	 */
	void addCourse(Course course);

	/**
	 * 分页获取学科列表
	 * @param queryPageBean
	 * @return
	 */
	PageResult findListByPage(QueryPageBean queryPageBean);

	/**
	 * 更新学科
	 * @param course
	 */
	void updateCourse(Course course);

	/**
	 * 删除某一学科
	 * @param id
	 */
	void deleteCourse(Integer id);

	/**
	 * 获取全部学科及目录列表
	 * 为试题录入
	 * @return
	 */
	List<Course> findListAll();
}
