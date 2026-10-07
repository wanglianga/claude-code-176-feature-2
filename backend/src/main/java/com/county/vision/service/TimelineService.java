package com.county.vision.service;

import com.county.vision.domain.entity.HealthEvent;
import com.county.vision.domain.entity.IssueTracker;
import com.county.vision.domain.entity.Student;
import com.county.vision.domain.enums.EventType;
import com.county.vision.domain.enums.ScreenFlag;
import com.county.vision.domain.enums.StudentStage;
import com.county.vision.domain.repository.HealthEventRepository;
import com.county.vision.domain.repository.IssueTrackerRepository;
import com.county.vision.domain.repository.StudentRepository;
import com.county.vision.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 时间线事件、异常事项、学生阶段/标记的统一处理 */
@Service
public class TimelineService {

    private final HealthEventRepository eventRepo;
    private final IssueTrackerRepository issueRepo;
    private final StudentRepository studentRepo;

    public TimelineService(HealthEventRepository eventRepo, IssueTrackerRepository issueRepo,
                           StudentRepository studentRepo) {
        this.eventRepo = eventRepo;
        this.issueRepo = issueRepo;
        this.studentRepo = studentRepo;
    }

    @Transactional
    public void event(Student student, EventType type, String content) {
        HealthEvent e = new HealthEvent();
        e.setStudent(student);
        e.setType(type);
        e.setEventTime(LocalDateTime.now());
        e.setContent(content);
        e.setActor(SecurityUtils.actorLabel());
        eventRepo.save(e);
    }

    /** 打开（或复用未闭环的）异常事项，并把标记累计到学生 flags */
    @Transactional
    public IssueTracker raiseIssue(Student student, ScreenFlag flag, String description) {
        addFlag(student, flag);
        return issueRepo.findFirstByStudentIdAndIssueTypeAndStatus(student.getId(), flag.name(), "OPEN")
                .orElseGet(() -> {
                    IssueTracker it = new IssueTracker();
                    it.setStudent(student);
                    it.setIssueType(flag.name());
                    it.setDescription(description);
                    it.setStatus("OPEN");
                    it.setRaisedBy(SecurityUtils.actorLabel());
                    it.setRaisedAt(LocalDateTime.now());
                    return issueRepo.save(it);
                });
    }

    /**
     * 打开异常事项；若已存在同类未闭环事项（如家长第二次拒绝复诊），不覆盖、不闭环原单，
     * 而是把新情况追加到描述末尾并刷新提出时间，保留完整累计过程。
     */
    @Transactional
    public IssueTracker raiseIssueAppend(Student student, ScreenFlag flag, String description) {
        addFlag(student, flag);
        return issueRepo.findFirstByStudentIdAndIssueTypeAndStatus(student.getId(), flag.name(), "OPEN")
                .map(it -> {
                    String base = it.getDescription() == null ? "" : it.getDescription();
                    if (!base.contains(description)) {
                        it.setDescription(base + " ｜ " + description);
                        it.setRaisedAt(LocalDateTime.now());
                        it.setRaisedBy(SecurityUtils.actorLabel());
                        issueRepo.save(it);
                    }
                    return it;
                })
                .orElseGet(() -> raiseIssue(student, flag, description));
    }

    /** 若该学生存在指定类型的未闭环异常事项，则闭环它（找不到不报错）。返回是否实际闭环。 */
    @Transactional
    public boolean resolveByFlag(Student student, ScreenFlag flag, String note) {
        return issueRepo.findFirstByStudentIdAndIssueTypeAndStatus(
                        student.getId(), flag.name(), "OPEN")
                .map(it -> {
                    resolveIssue(it.getId(), note);
                    return true;
                }).orElse(false);
    }

    @Transactional
    public void addFlag(Student student, ScreenFlag flag) {
        Set<String> flags = new LinkedHashSet<>(splitFlags(student.getFlags()));
        if (flags.add(flag.name())) {
            student.setFlags(String.join(",", flags));
            studentRepo.save(student);
        }
    }

    @Transactional
    public void removeFlag(Student student, ScreenFlag flag) {
        Set<String> flags = new LinkedHashSet<>(splitFlags(student.getFlags()));
        flags.remove(flag.name());
        student.setFlags(String.join(",", flags));
        studentRepo.save(student);
    }

    @Transactional
    public void changeStage(Student student, StudentStage stage) {
        StudentStage old = student.getStage();
        student.setStage(stage);
        studentRepo.save(student);
        if (old != stage) {
            event(student, EventType.STAGE_CHANGE, "状态流转：" + old.getLabel() + " → " + stage.getLabel());
        }
    }

    @Transactional
    public IssueTracker resolveIssue(Long issueId, String note) {
        IssueTracker issue = issueRepo.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("异常事项不存在"));
        issue.setStatus("RESOLVED");
        issue.setResolvedBy(SecurityUtils.actorLabel());
        issue.setResolvedAt(LocalDateTime.now());
        issue.setResolveNote(note);
        issueRepo.save(issue);

        Student s = issue.getStudent();
        // 对应异常标记移除（若无其他同类未闭环事项）
        boolean stillOpen = issueRepo.findByStudentIdOrderByRaisedAtDesc(s.getId()).stream()
                .anyMatch(i -> i.getIssueType().equals(issue.getIssueType()) && "OPEN".equals(i.getStatus()));
        if (!stillOpen) {
            try {
                removeFlag(s, ScreenFlag.valueOf(issue.getIssueType()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        event(s, EventType.RESOLVE, "异常事项已闭环【" + labelOf(issue.getIssueType()) + "】" + nullSafe(note));
        return issue;
    }

    public static List<String> splitFlags(String flags) {
        if (flags == null || flags.isBlank()) return List.of();
        return Arrays.stream(flags.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    public static String labelOf(String flag) {
        try {
            return ScreenFlag.valueOf(flag).getLabel();
        } catch (Exception e) {
            return flag;
        }
    }

    private String nullSafe(String s) {
        return s == null ? "" : "：" + s;
    }
}
