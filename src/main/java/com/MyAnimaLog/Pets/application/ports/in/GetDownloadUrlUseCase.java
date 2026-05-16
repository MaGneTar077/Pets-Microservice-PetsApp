package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.GetDownloadUrlRequest;
import com.MyAnimaLog.Pets.application.dto.GetDownloadUrlResponse;

public interface GetDownloadUrlUseCase {
    GetDownloadUrlResponse getDownloadUrl(GetDownloadUrlRequest request);
}
