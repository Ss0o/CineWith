// Display-only check: nicknames are currently unique and cannot be changed.
// The server independently authorizes every mutation using the session member ID.
export function isOwnContent(auth, content) {
  return auth.status === 'MEMBER'
    && typeof auth.member?.nickname === 'string'
    && auth.member.nickname === content?.author?.nickname
}
