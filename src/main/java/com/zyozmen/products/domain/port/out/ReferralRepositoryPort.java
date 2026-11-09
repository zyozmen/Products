package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Referral;
import com.zyozmen.products.domain.model.ReferralPage;

public interface ReferralRepositoryPort {
    void insert(Referral referral);

    ReferralPage findByReferenteId(String idReferente, int page, int size);
}
