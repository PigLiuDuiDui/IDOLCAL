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
    clickForDetails: '자세히 보기'
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
