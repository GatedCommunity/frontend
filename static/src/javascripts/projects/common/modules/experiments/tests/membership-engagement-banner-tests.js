define([
    'bean',
    'qwery',
    'common/utils/config',
    'common/utils/storage',
    'common/utils/template',
    'common/modules/commercial/commercial-features',
    'common/modules/commercial/user-features',
    'common/utils/mediator'
], function (
    bean,
    qwery,
    config,
    storage,
    template,
    commercialFeatures,
    userFeatures,
    mediator
) {
    var EditionTest = function (edition, campaignPrefix) {

        this.edition = edition;
        this.id = 'MembershipEngagementBanner'+edition[0].toUpperCase() + edition.substr(1);
        this.start = '2016-09-08';
        this.expiry = '2016-10-08';
        this.author = 'Roberto Tyley';
        this.description = 'Show contributions/membership messages for the ' + edition + ' edition.';
        this.showForSensitive = false;
        this.audience = 1.0;
        this.audienceOffset = 0;
        this.successMeasure = 'Conversion for contributions';
        this.audienceCriteria = 'All users in the ' + edition + ' edition.';
        this.dataLinkNames = '';
        this.idealOutcome = '';

        // Required by the A/B testing framework - can not be async, unfortunately
        this.canRun = function () {
            var matchesEdition = config.page.edition.toLowerCase() == edition;
            return matchesEdition && commercialFeatures.canReasonablyAskForMoney;
        };

        this.completer = function (complete) {
            mediator.on('membership-message:display', function () {
                /* When the button link is clicked, call the function that indicates the A/B test is 'complete'
                 * ...note that for Membership & Contributions this completion is only the start of a longer
                 * journey that will hopefully end pages later with the user giving us money.
                 */
                bean.on(qwery('#membership__engagement-message-link')[0], 'click', complete);
            });
        };

        this.variants = [];

    };


    EditionTest.prototype.addMessageVariant = function (id, variantParams, linkHref, cssModifierClass) {
        var self = this;

        this.variants.push({
            id: id,
            variantParams: variantParams,
            /*  We don't want to run any 'code' in this test, we just want a variant to be selected. All message display
             * is performed in membership-engagement-banner.js, modifying the banner using the data in variantParams.
             */
            test: function () {},
            success: this.completer
        });
        return this;
    };

    EditionTest.prototype.addMembershipVariant = function (suffix, variantParams) {
        var id = campaignPrefix + suffix;

        var colours = ['default','vibrant-blue','yellow','light-blue','deep-purple','teal'];

        // Rotate through different colours on successive page views
        var colourIndex = storage.local.get('gu.alreadyVisited') % colours.length;
        var cssModifierClass = 'membership-message-' + colours[colourIndex];

        return this.addMessageVariant(id, variantParams, 'https://membership.theguardian.com/'+this.edition+'/supporter?INTCMP=' + id, cssModifierClass);
    };

    EditionTest.prototype.addContributionsVariant = function (suffix, variantParams) {
        var id = campaignPrefix + suffix;
        return this.addMessageVariant(id, variantParams, 'https://contribute.theguardian.com/'+this.edition+'?INTCMP='+id, 'contributions-message');
    };

    return [
        new EditionTest('uk', 'gdnwb_copts_mem_banner_ukbanner__')
            .addMembershipVariant('post_truth_world', {messageText: 'In a post-truth world, facts matter more than ever. Support the Guardian for £5 a month'})
            .addMembershipVariant('now_is_the_time', {messageText: 'If you’ve been thinking about supporting us, now is the time to do it. Support the Guardian for £5 a month'})
            .addMembershipVariant('everyone_chipped_in', {messageText: 'Not got around to supporting us yet? If everyone chipped in, our future would be more secure. Support the Guardian for £5 a month'})
            .addMembershipVariant('free_and_open', {messageText: 'By giving £5 a month you can help to keep the Guardian’s journalism free and open for all'})
        ,new EditionTest('au', 'gdnwb_copts_mem_banner_aubanner__')
            .addMembershipVariant('fearless_10', {messageText: 'We need you to help support our fearless independent journalism. Become a Guardian Australia member for just $10 a month'})
            .addMembershipVariant('stories_that_matter', {messageText: 'We need your help to tell the stories that matter. Support Guardian Australia now'})
            .addMembershipVariant('power_to_account', {messageText: 'We need your help to hold power to account. Become a Guardian Australia supporter'})
            .addMembershipVariant('independent_journalism', {messageText: 'Support quality, independent journalism in Australia by becoming a supporter'})
        ,new EditionTest('int', 'gdnwb_copts_mem_banner_ROWbanner__')
            .addMembershipVariant('10th_article', {minArticles: 10})
            .addMembershipVariant('1st_article', {minArticles: 1})
    ];
});
