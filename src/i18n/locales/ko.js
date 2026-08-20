// 한국어 언어팩
export default {
  meta: {
    title: 'EVAN 공식 일정',
    description:
      'EVAN 공식 일정 — 공식 일정 아카이브: 컴백, 음악방송, 라이브, 오프라인 행사, 브랜드 활동, 생일 등'
  },
  nav: {
    label: '주요 내비게이션',
    schedule: '일정',
    comeback: '컴백',
    reminders: '알림',
    timeline: '타임라인',
    archive: '아카이브',
    about: '소개'
  },
  bottomNav: {
    label: '하단 내비게이션'
  },
  common: {
    all: '전체',
    language: '언어'
  },
  hero: {
    officialArchive: '공식 아카이브 — {year}',
    currentEra: '현재 시대',
    nextEvent: '다음 일정',
    clickForDetails: '자세히 보기',
    remindButton: '알림 설정'
  },
  subscribe: {
    button: '일정 구독',
    eyebrow: '달력 구독',
    title: '휴대폰 달력에 구독',
    desc: '{artist}의 일정을 휴대폰 달력에 동기화하세요. 일정이 업데이트되면 자동으로 새로고침되고, 시간이 되면 휴대폰이 알려줍니다.',
    copy: '링크 복사',
    copied: '복사됨',
    copyFail: '복사 실패 — 링크를 길게 눌러 복사',
    download: '.ics 파일 다운로드',
    stepIos: 'iPhone: 링크 복사 → 설정 → 캘린더 → 구독 캘린더 추가',
    stepAndroid: 'Android: 링크 복사 → 달력 앱 → 구독 추가 (기기별로 경로 상이)',
    localHint: '현재 로컬 미리보기 모드입니다. 배포 후 정식 구독 링크가 활성화됩니다.',
    deployHint: '구독 링크를 팬들과 공유하세요. 한 번 추가하면 자동으로 계속 동기화됩니다.',
    close: '닫기',
    alarmLabel: '다운로드 알림',
    alarmNone: '알림 없음',
    alarmStatic: '구독 링크는 고정 파일로 기본 알림이 내장됩니다. .ics 다운로드로 이벤트별 알림을 직접 설정할 수 있습니다.'
  },
  calendar: {
    prevMonth: '이전 달',
    nextMonth: '다음 달',
    switchView: '보기 전환',
    today: '오늘',
    month: '월',
    list: '목록',
    more: '＋{n}개 더보기',
    empty: '선택한 유형에 일정이 없습니다.'
  },
  home: {
    switchView: '보기 전환',
    today: '오늘',
    thisWeek: '이번 주',
    calendar: '달력',
    todayLabel: '오늘',
    thisWeekLabel: '이번 주',
    todayTag: '오늘',
    todayEmpty: '오늘 일정이 없습니다. 잠시 휴식 \u2014 또는 이번 주 일정을 확인해 보세요.',
    weekEmpty: '향후 7일간 예정된 일정이 없습니다.',
    allDay: '종일'
  },
  reminder: {
    setBtn: '알림 받기',
    setBtnDone: '알림 설정됨',
    title: '알림 설정',
    eyebrow: '일정 알림',
    at: '에',
    confirm: '알림 설정',
    update: '알림 수정',
    cancel: '알림 취소',
    close: '닫기',
    setAt: '알림 설정됨: {at}',
    noTime: '아직 시작 시간이 확정되지 않은 일정입니다.',
    options: {
      '1d': '하루 전',
      '3h': '3시간 전',
      '1h': '1시간 전',
      '30m': '30분 전',
      start: '시작 시간에',
      custom: '직접 설정'
    },
    units: {
      minute: '분',
      hour: '시간',
      day: '일'
    },
    customHint: '5분 ~ 30일 전',
    beforeMin: '{n}분 전',
    beforeHour: '{n}시간 전',
    beforeDay: '{n}일 전'
  },
  reminders: {
    eyebrow: '내 알림',
    sub: '설정한 모든 일정 알림을 한곳에서 확인하세요. 시스템 푸시를 켜면 사이트를 열지 않아도 알림을 받을 수 있습니다.',
    groupToday: '오늘',
    groupTomorrow: '내일',
    groupUpcoming: '다가오는 알림',
    alertAt: '알림 시각',
    cancel: '취소',
    emptyTitle: '아직 알림이 없습니다',
    emptyDesc: '일정을 열고 \u201c알림 받기\u201d를 눌러 시작 전에 알림을 받아 보세요.',
    browse: '일정 보기',
    note: '알림은 공식 시간대 기준으로 계산되며 사용자 시간대로 변환되어 표시됩니다.',
    select: '선택',
    selectAll: '전체 선택',
    selected: '{n}개 선택됨',
    batchSet: '일괄 설정',
    batchCancel: '선택 알림 취소',
    batchTitle: '알림 시간 일괄 설정',
    batchDesc: '선택한 {n}개의 알림에 일괄 적용:',
    applied: '{n}개의 알림이 업데이트됨',
    noSelection: '일괄 설정할 알림을 먼저 선택하세요',
    apply: '적용',
    done: '완료'
  },
  push: {
    eyebrow: '푸시 알림',
    title: '시스템 푸시 알림',
    desc: '사이트를 열지 않아도 일정 시작 전에 시스템 알림을 받을 수 있습니다.',
    on: '푸시 켜짐',
    off: '푸시 꺼짐',
    enable: '푸시 켜기',
    disable: '푸시 끄기',
    busy: '처리 중…',
    test: '테스트 알림 보내기',
    testing: '보내는 중…',
    sent: '테스트 알림이 전송되었습니다',
    failed: '실패: {msg}',
    unsupported: '이 브라우저는 Web Push를 지원하지 않습니다. 최신 Chrome / Edge / Safari를 사용해 주세요.',
    iosGuide: 'iPhone / iPad에서는 Safari에서 이 사이트를 홈 화면에 추가한 뒤, 홈 화면 아이콘으로 열어야 푸시를 켤 수 있습니다.',
    iosGuide2: 'Safari → 공유 버튼 → 홈 화면에 추가',
    androidHint: '버튼을 누르면 브라우저가 권한을 요청합니다. “허용”을 선택하세요.',
    needReminders: '일정에 알림을 설정하면 선택한 시간에 푸시 알림이 도착합니다.'
  },
  timezone: {
    select: '시간대 선택',
    eyebrow: '시간대',
    auto: '자동',
    autoHint: '기기 시간대 따르기',
    official: '공식 시간',
    yourTime: '내 시간',
    close: '닫기'
  },
  comeback: {
    eyebrow: '컴백',
    releaseDate: '발매일',
    timeline: '컴백 타임라인',
    timelineNote: '전체 일정',
    statuses: {
      COMPLETED: '완료',
      UPCOMING: '예정',
      TODAY: '오늘'
    },
    stages: {
      'concept-photo': '컨셉 포토',
      tracklist: '트랙리스트',
      'highlight-medley': '하이라이트 메들리',
      'mv-teaser': 'MV 티저',
      'album-release': '앨범 발매',
      showcase: '쇼케이스',
      'music-shows': '음악방송'
    }
  },
  upcoming: {
    title: '다가오는 일정',
    note: '다음 7개 일정',
    next: 'NEXT',
    empty: '선택한 유형의 다가오는 일정이 없습니다.'
  },
  timeline: {
    eyebrow: '일정 아카이브',
    sub: '전체 활동 기간의 연대기 아카이브. {artist}의 공식 일정을 월별로 정리했습니다 — {era} era.',
    eventCount: '일정 {n}개',
    empty: '선택한 유형에 일정이 없습니다.',
    browseArchive: '전체 아카이브 보기 →'
  },
  archive: {
    eyebrow: '공식 아카이브 — {year}',
    sub: '지난 공식 일정 아카이브. 모든 항목은 원본 공식 출처 기록을 유지하며, 절대 날조하거나 다시 쓰지 않습니다.',
    empty: '선택한 유형의 지난 일정이 없습니다.'
  },
  about: {
    eyebrow: '이 아카이브에 대해',
    sourcePolicy: '출처 정책',
    note: '이 아카이브 사이트는 정보를 만들지 않고 기록만 합니다. 모든 일정에는 공식 출처와 신뢰도 등급이 표시되며, 비공식 내용을 공식 정보로 위장하지 않습니다.',
    levels: {
      official: {
        name: '공식 채널',
        desc: 'Weverse / 공식 SNS / 공식 YouTube 등 아티스트 공식 채널에서 제공되며 신뢰도가 가장 높습니다. 공식 발표를 기준으로 합니다.'
      },
      brand: {
        name: '브랜드 채널',
        desc: '협력 브랜드의 공식 채널에서 제공됩니다. 활동 자체는 공식 협업이며, 정보는 브랜드 발표를 기준으로 합니다.'
      },
      media: {
        name: '미디어 채널',
        desc: 'TV 프로그램, 잡지, 미디어 플랫폼에서 제공됩니다. 공식이 참여한 콘텐츠이며, 발표 채널은 제3자입니다.'
      },
      fan: {
        name: '팬 프로젝트',
        desc: '공식적으로 확인되지 않은 정보는 모두 RUMORED로 표시되며, 참고용일 뿐 공식 일정으로 간주하지 않습니다.'
      }
    },
    footer:
      '이 페이지는 팬이 직접 만든 공식 일정 아카이브 사이트로, {artist} 및 소속사와 아무런 관련이 없습니다. 모든 일정은 공식 채널 발표를 기준으로 합니다.',
    footNote: 'EVAN OFFICIAL SCHEDULE — ARCHIVE {year}'
  },
  drawer: {
    label: '일정 상세',
    close: '닫기',
    date: '날짜',
    time: '시간',
    location: '장소',
    countdown: '카운트다운',
    officialSource: '공식 출처',
    published: '게시일 {date}',
    viewOriginal: '원문 보기 →',
    viewOfficialSource: '공식 출처 보기',
    viewSource: '출처 보기',
    watchLive: '라이브 시청',
    joinOnline: '온라인 참여',
    viewLocation: '장소 보기',
    navigate: '길찾기',
    addToCalendar: '내 캘린더에 추가'
  },
  types: {
    RELEASE: '컴백',
    EVENT: '이벤트',
    TV: '예능',
    LIVE: '라이브',
    PHOTO: '화보',
    MAGAZINE: '매거진',
    OFFLINE: '오프라인',
    BRAND: '브랜드',
    BIRTHDAY: '생일'
  },
  status: {
    CONFIRMED: '확정',
    TBA: '미정',
    RUMORED: '루머'
  },
  sourceLevels: {
    OFFICIAL: '검증된 출처',
    BRAND: '브랜드 출처',
    MEDIA: '미디어 출처',
    FAN: '팬 프로젝트'
  }
}
