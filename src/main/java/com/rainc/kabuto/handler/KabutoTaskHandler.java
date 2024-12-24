package com.rainc.kabuto.handler;

import com.rainc.kabuto.entity.KabutoTask;
import com.rainc.kabuto.enums.KabutoTaskStatusEnum;

public interface KabutoTaskHandler {


	KabutoTaskStatusEnum run(KabutoTask task) throws Exception;

	/**
	 * 任务执行成功后会调用一次
	 *
	 * @param task
	 */
	default void onSuccess(KabutoTask task) {
	}

	/**
	 * 任务最后一次重试失败后调用
	 *
	 * @param task
	 */
	default void onFinallyFail(KabutoTask task) {
	}

}
