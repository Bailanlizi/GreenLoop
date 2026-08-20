package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.entity.OutboxEvent;
import com.campus.trade.mapper.OutboxEventMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/notifications/outbox")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOutboxController {
    private final OutboxEventMapper mapper;
    public AdminOutboxController(OutboxEventMapper mapper) { this.mapper = mapper; }
    @GetMapping public Result<List<OutboxEvent>> list(@RequestParam(required = false) String status) { return Result.success(mapper.findAll(status)); }
    @PostMapping("/{id}/replay") public Result<Void> replay(@PathVariable Long id) { if (mapper.requeue(id) != 1) return Result.error("仅死信事件可重放"); return Result.success(); }
}
