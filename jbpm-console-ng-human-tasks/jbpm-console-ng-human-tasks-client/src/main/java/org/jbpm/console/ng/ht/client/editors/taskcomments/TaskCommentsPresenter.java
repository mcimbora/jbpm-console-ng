/*
 * Copyright 2013 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.console.ng.ht.client.editors.taskcomments;

import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jbpm.console.ng.ht.client.i18n.Constants;
import org.jbpm.console.ng.ht.model.CommentSummary;
import org.jbpm.console.ng.ht.model.events.TaskRefreshedEvent;
import org.jbpm.console.ng.ht.model.events.TaskSelectionEvent;
import org.jbpm.console.ng.ht.service.TaskService;
import org.uberfire.client.mvp.UberView;
import org.uberfire.ext.widgets.common.client.callbacks.DefaultErrorCallback;

@Dependent
public class TaskCommentsPresenter {

    public interface TaskCommentsView extends UberView<TaskCommentsPresenter> {

        void clearCommentInput();

        void redrawDataGrid();

        void displayNotification(String text);
    }

    private Constants constants = Constants.INSTANCE;
    private final TaskCommentsView view;
    private final Caller<TaskService> taskService;
    private final ListDataProvider<CommentSummary> dataProvider = new ListDataProvider<CommentSummary>();
    private long currentTaskId = 0;
    private String serverTemplateId;
    private String containerId;

    @Inject
    public TaskCommentsPresenter(TaskCommentsView view, Caller<TaskService> taskService) {
        this.view = view;
        this.taskService = taskService;
    }

    @PostConstruct
    public void init() {
        view.init(this);
    }

    public IsWidget getView() {
        return view;
    }

    public ListDataProvider<CommentSummary> getDataProvider() {
        return dataProvider;
    }

    public void refreshComments() {
        taskService.call(
                new RemoteCallback<List<CommentSummary>>() {
                    @Override
                    public void callback(List<CommentSummary> comments) {
                        dataProvider.getList().clear();
                        dataProvider.getList().addAll(comments);
                        view.redrawDataGrid();
                    }
                },
                new DefaultErrorCallback()
        ).getTaskComments(serverTemplateId, containerId, currentTaskId);
    }

    public void addTaskComment(final String text) {
        if ("".equals(text.trim())) {
            view.displayNotification(constants.CommentCannotBeEmpty());
        } else {
            addTaskComment(text, new Date());
        }
    }

    private void addTaskComment(final String text, final Date addedOn) {
        taskService.call(
                new RemoteCallback<Void>() {
                    @Override
                    public void callback(Void response) {
                        refreshComments();
                        view.clearCommentInput();
                    }
                },
                new DefaultErrorCallback()
        ).addTaskComment(serverTemplateId, containerId, currentTaskId, text, addedOn);
    }

    public void removeTaskComment(long commentId) {
        taskService.call(
                new RemoteCallback<Void>() {
                    @Override
                    public void callback(Void response) {
                        refreshComments();
                        view.clearCommentInput();
                        view.displayNotification(constants.CommentDeleted());
                    }
                },
                new DefaultErrorCallback()
        ).deleteTaskComment(serverTemplateId, containerId, currentTaskId, commentId);
    }

    public void addDataDisplay(final HasData<CommentSummary> display) {
        dataProvider.addDataDisplay(display);
    }

    public void onTaskSelectionEvent(@Observes final TaskSelectionEvent event) {
        currentTaskId = event.getTaskId();
        serverTemplateId = event.getServerTemplateId();
        containerId = event.getContainerId();
        refreshComments();
    }

    public void onTaskRefreshedEvent(@Observes final TaskRefreshedEvent event) {
        if (currentTaskId == event.getTaskId()) {
            refreshComments();
        }
    }
}
