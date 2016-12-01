define([
    'bean',
    'reqwest',
    'fastdom',
    'qwery',
    'common/utils/$',
    'common/utils/config',
    'common/modules/commercial/commercial-features',
    'common/utils/mediator'
], function (
    bean,
    reqwest,
    fastdom,
    qwery,
    $,
    config,
    commercialFeatures,
    mediator
) {
    return function () {
        this.id = 'MembershipEngagementInternationalExperiment';
        this.start = '2016-11-10';
        this.expiry = '2016-12-1';
        this.author = 'Rupert Bates';
        this.description = 'Test varying the number of visits before showing the membership engagement banner';
        this.audience = 1;    // 100% (of International audience)
        this.audienceOffset = 0;
        this.successMeasure = 'More membership sign-ups';
        this.audienceCriteria = '100 percent of (non-member) International edition readers';
        this.dataLinkNames = '';
        this.idealOutcome = 'More readers engage with the banner and then complete membership sign-up';
        this.hypothesis = 'Showing the banner to users who have visited us less frequently gives us a larger pool of potential supporters';

        this.canRun = function () {
            return config.page.edition.toLowerCase() === 'int' &&
                commercialFeatures.canReasonablyAskForMoney;
        };

        var success = function (complete) {
            if (this.canRun()) {
                mediator.on('membership-message:display', function () {
                    bean.on(qwery('#membership__engagement-message-link')[0], 'click', complete);
                });
            }
        };

        this.variants = [
            {
                id: '10th_article',
                test: function () {},
                success: success.bind(this)
            },
            {
                id: '1st_article',
                test: function () {},
                success: success.bind(this)
            }
        ];
    };
});
