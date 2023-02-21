CREATE TABLE IF NOT EXISTS userLangs(
    lang char(50) NOT NULL,
    uuid char(36) NOT NULL,
    PRIMARY KEY (uuid)
);

CREATE TABLE IF NOT EXISTS translations(
  lang char(50) NOT NULL,
  messKey char(255) NOT NULL,
  msg LONGTEXT,
  lore LONGTEXT,
  PRIMARY KEY (messKey, lang)
);
