package de.tectoast.emolga.domain.league.draft.model.permission

import de.tectoast.emolga.features.league.draft.K18n_DraftPermission
import de.tectoast.emolga.features.system.model.Nameable
import de.tectoast.k18n.generated.K18nMessage

enum class DraftMention(override val prettyName: K18nMessage, val selfmention: Boolean, val othermention: Boolean) :
    Nameable {
    ME(K18n_DraftPermission.AllowMentionMe, selfmention = true, othermention = false),
    OTHER(K18n_DraftPermission.AllowMentionOther, selfmention = false, othermention = true),
    BOTH(K18n_DraftPermission.AllowMentionBoth, selfmention = true, othermention = true)
}