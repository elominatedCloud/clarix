package com.clarix.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.clarix.domain.ClinicalNote;
import com.clarix.domain.User;
import com.clarix.repo.ClinicalNoteRepository;

/**
 * 노트 수정 — 작성자 본인만 자기 노트를 수정할 수 있다.
 * 의사가 간호 노트를 수정하지 못하고, 그 반대도 마찬가지.
 */
@Service
public class NoteService {

    private final ClinicalNoteRepository notes;

    public NoteService(ClinicalNoteRepository notes) {
        this.notes = notes;
    }

    public ClinicalNote requireOwn(User author, UUID noteId) {
        ClinicalNote n = notes.findById(noteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "노트 없음"));
        if (!n.getDoctor().getId().equals(author.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 작성한 노트만 수정 가능");
        }
        return n;
    }

    @Transactional
    public ClinicalNote update(User author, UUID noteId,
                               String s, String o, String a, String p) {
        ClinicalNote n = requireOwn(author, noteId);
        n.setSubjective(s); n.setObjective(o); n.setAssessment(a); n.setPlan(p);
        n.setUpdatedAt(OffsetDateTime.now());
        return notes.save(n);
    }
}
