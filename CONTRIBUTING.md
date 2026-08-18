# Quy trình Git — Gitflow

Repo: [hoclaptrinh33/BTL_Java_Web_QlyKiTucXa](https://github.com/hoclaptrinh33/BTL_Java_Web_QlyKiTucXa)

**GitHub đã khóa `main` và `develop`.** `git push origin main` / `git push origin develop` sẽ bị từ chối. Phải làm trên nhánh riêng rồi mở Pull Request.

Issue để nhận việc: [Issues](https://github.com/hoclaptrinh33/BTL_Java_Web_QlyKiTucXa/issues) · bảng việc [#40](https://github.com/hoclaptrinh33/BTL_Java_Web_QlyKiTucXa/issues/40).

---

## 1. Mô hình nhánh (Gitflow)

```
          hotfix/login-csrf ──────────────► main
                 ▲                           │
                 │                           │ tag v0.1 (demo)
                 │                           ▼
main ──────────────────────────────────────────────  (luôn chạy được / sẵn sàng chấm)
                 ▲
                 │  PR release (khi demo / chốt mốc)
                 │
develop ─────────┴────────────────────────────────  (tích hợp hàng ngày)
   ▲          ▲          ▲
   │          │          │
feature/2-flyway-v1   feature/7-register   feature/11-crud-toa
```

| Nhánh | Vai trò | Ai được đẩy thẳng? |
| --- | --- | --- |
| `main` | Code ổn định, sẵn sàng demo | **Không ai.** Chỉ nhận PR từ `develop` hoặc `hotfix/*` |
| `develop` | Nhánh tích hợp của nhóm | **Không ai.** Chỉ nhận PR từ `feature/*` / `bugfix/*` |
| `feature/<issue>-ten` | Một issue / một tính năng | Chủ nhánh (qua PR vào `develop`) |
| `bugfix/<issue>-ten` | Sửa lỗi chưa ra `main` | PR vào `develop` |
| `hotfix/<ten>` | Sửa gấp trên bản đang demo | PR vào `main` **và** cherry-pick / PR ngược về `develop` |
| `release/x.y` | (Tuỳ chọn) đóng gói buổi chấm | PR `develop` → `release/*` → `main` |

Nhánh mặc định khi clone: **`develop`**.

### GitHub chặn gì?

Ruleset **Protect main and develop** (Settings → Rules):

| Thao tác | Kết quả |
| --- | --- |
| `git push origin main` hoặc `git push origin develop` | **Bị chặn** — kể cả admin |
| `git push --force` lên `main` / `develop` | **Bị chặn** |
| Xóa nhánh `main` / `develop` | **Bị chặn** |
| `git push origin feature/2-flyway-v1` | Được |
| Mở PR `feature/…` → `develop` rồi bấm Merge | Được |
| Mở PR `develop` → `main` (release) hoặc `hotfix/…` → `main` | Được |

Lỗi điển hình khi push thẳng:

```text
remote: error: GH013: Repository rule violations found for refs/heads/develop.
remote: - Changes must be made through a pull request.
```

Cách đúng: quay lại nhánh feature, push nhánh đó, tạo PR.

---

## 2. Lần đầu — clone code mới nhất

Cài [Git](https://git-scm.com/download/win) + tài khoản GitHub đã được add vào repo.

```powershell
cd E:\lehai\Documents\Project
git clone git@github.com:hoclaptrinh33/BTL_Java_Web_QlyKiTucXa.git
cd BTL_Java_Web_QlyKiTucXa
git checkout develop
git pull origin develop
```

HTTPS nếu chưa cấu hình SSH:

```powershell
git clone https://github.com/hoclaptrinh33/BTL_Java_Web_QlyKiTucXa.git
```

Kiểm tra:

```powershell
git remote -v
git branch -vv
git status
```

Phải thấy `develop` theo dõi `origin/develop`. **Không làm việc trên `main`.**

---

## 3. Mỗi khi bắt đầu một task

1. Vào issue, comment `Nhận task`, self-assign.
2. Lấy code `develop` mới nhất (người khác có thể đã merge).
3. Tạo nhánh **từ `develop`**, đúng issue.

```powershell
git checkout develop
git pull origin develop
git checkout -b feature/2-flyway-v1
```

### Đặt tên nhánh

```
feature/<số-issue>-<ten-ngan-khong-dau>
bugfix/<số-issue>-<ten-ngan>
hotfix/<ten-ngan>
```

| Đúng | Sai |
| --- | --- |
| `feature/2-flyway-v1` | `main`, `develop` |
| `feature/7-register-sv` | `hai-lam-auth` (thiếu số issue) |
| `bugfix/15-staff-thieu-toa` | `feature/all-module-auth` (ôm nhiều issue) |
| `hotfix/login-csrf` | `feature/Feature/Login` |

Một nhánh = **một issue**. Không nhét nhiều issue vào một PR.

---

## 4. Làm việc và commit

Lặp lại: sửa code → xem diff → commit **trên nhánh feature**.

```powershell
git status
git diff
git add src/main/resources/db/migration/V1__init.sql
git commit -m "feat(db): them Flyway V1 schema users den system_locks"
```

- Chỉ `git add` file mình sửa. Không add `.env`, `application-local.yml`, `target/`.
- Commit **nhiều lần nhỏ** trên nhánh feature cũng được; lúc mở PR nên gọn, dễ review.

### Message commit

```
<type>(<module>): mô tả ngắn, tiếng Việt không dấu hoặc tiếng Anh
```

| type | Khi nào |
| --- | --- |
| `feat` | Tính năng mới |
| `fix` | Sửa lỗi |
| `chore` | Tooling, seeder, pom |
| `docs` | Tài liệu |
| `test` | Thêm / sửa test |
| `refactor` | Đổi code, không đổi hành vi |

Module lấy từ label issue: `db`, `auth`, `users`, `infra`, `apply`, `alloc`, `contract`, `billing`, `ops`, `report`, `config`, `ux`.

Ví dụ khớp repo:

```
feat(auth): form login CSRF va redirect theo role
fix(infra): chan xoa phong khi con bed OCCUPIED
docs: huong dan Gitflow cho nhom
test(alloc): rank score va SOFT khong noi gender
```

**Không** commit kiểu `update`, `fix bug`, `asdasd`.

---

## 5. Đẩy nhánh và mở Pull Request

Chưa xong hết issue cũng có thể push nhánh (backup). Khi xong tiêu chí issue → mở PR.

```powershell
git push -u origin feature/2-flyway-v1
```

Tạo PR **vào `develop`**, không vào `main`:

```powershell
gh pr create --base develop --title "feat(db): Flyway V1 schema" --body "Closes #2"
```

Hoặc trên GitHub: **Compare & pull request** → base = **`develop`** ← compare = `feature/2-flyway-v1`.

### Checklist trước khi bấm Create

- [ ] Base là `develop` (hotfix thì base `main`)
- [ ] Body có `Closes #<số>` để GitHub tự đóng issue khi merge
- [ ] Đã `git pull origin develop` và giải conflict (nếu có)
- [ ] Mô tả: làm gì, đọc file đặc tả nào, cách test
- [ ] Không chứa secret / mật khẩu / file IDE

Mẫu mô tả nằm ở `.github/PULL_REQUEST_TEMPLATE.md`.

### Review

- Ít nhất **một thành viên khác** xem rồi mới merge.
- Sửa theo comment: commit tiếp trên **cùng nhánh**, `git push` — PR tự cập nhật.
- Người mở PR **không tự merge** trừ khi nhóm thống nhất (task nhỏ, `docs`/`chore`).

---

## 6. Sau khi PR được merge

```powershell
git checkout develop
git pull origin develop
git branch -d feature/2-flyway-v1
git push origin --delete feature/2-flyway-v1
```

(Nếu đã bật xoá nhánh trên GitHub khi merge thì bỏ dòng `git push origin --delete`.)

Bắt đầu task tiếp theo: lặp **mục 3** từ `develop` mới.

---

## 7. Cập nhật nhánh khi `develop` đã đi tiếp

Nếu PR báo conflict hoặc bạn làm lâu:

```powershell
git checkout feature/2-flyway-v1
git fetch origin
git merge origin/develop
# sửa conflict → git add → git commit
git push
```

Không dùng `git push --force` lên `develop` / `main`. Force trên feature của mình chỉ khi nhóm đồng ý (sau rebase).

---

## 8. Cấm / được phép

| | |
| --- | --- |
| Cấm | `git push origin main` — GitHub từ chối |
| Cấm | `git push origin develop` — GitHub từ chối |
| Cấm | Commit trực tiếp trên `main` / `develop` rồi push |
| Cấm | Một PR sửa 5 issue |
| Được | `git push origin feature/...` rồi mở PR vào `develop` |
| Được | Nhiều commit nhỏ trên feature |
| Được | `git pull origin develop` mỗi ngày trước khi code |

---

## 9. Hotfix khi `main` đang demo bị lỗi

```powershell
git checkout main
git pull origin main
git checkout -b hotfix/login-csrf
# sửa → commit → push
gh pr create --base main --title "fix(auth): chan CSRF token het han khi login"
```

Sau khi merge vào `main`, **đưa fix về `develop`** (PR `hotfix/...` → `develop` hoặc cherry-pick) để không mất sửa.

---

## 10. Đưa `develop` lên `main` (mốc demo)

Khi M0/M1/… đủ để chạy:

1. Kiểm tra `develop` build/chạy được.
2. Mở PR **`develop` → `main`** (title: `release: M0 bootstrap va dang nhap`).
3. Review nhanh, merge.
4. (Tuỳ chọn) gắn tag: `git tag -a v0.1-m0 -m "M0"` rồi `git push origin v0.1-m0`.

Không merge từng `feature/*` thẳng vào `main`.

---

## 11. Lệnh hay dùng

```powershell
git status                          # đang ở nhánh nào, file nào đổi
git log --oneline -10               # 10 commit gần nhất
git checkout develop                # về nhánh tích hợp
git pull origin develop             # lấy code nhóm
git checkout -b feature/12-crud-phong
git add -p                          # chọn từng hunk để commit
git commit -m "feat(infra): ..."
git push -u origin HEAD             # đẩy nhánh hiện tại
gh pr create --base develop         # mở PR
```

Lạc nhánh / sợ hỏng:

```powershell
git status
git stash -u          # cất tạm (nếu chưa commit)
git checkout develop
git pull origin develop
```

Đừng `git reset --hard` nếu chưa chắc — hỏi nhóm trước.

---

## 12. Liên kết

- Đặc tả: [docs/README.md](./docs/README.md)
- PR plan: [docs/12-pr-plan.md](./docs/12-pr-plan.md)
- Issues: https://github.com/hoclaptrinh33/BTL_Java_Web_QlyKiTucXa/issues
